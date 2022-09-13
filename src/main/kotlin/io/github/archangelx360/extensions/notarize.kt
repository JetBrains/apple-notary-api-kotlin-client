package io.github.archangelx360.extensions

import io.github.archangelx360.NotaryClientV2
import io.github.archangelx360.models.Logs
import io.github.archangelx360.models.NewSubmissionRequest
import io.github.archangelx360.models.SubmissionResponse
import io.ktor.client.plugins.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.readBytes
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private val logger = LoggerFactory.getLogger("notary-api-kotlin-client")

data class NotarizationResult(
    val submissionId: String,
    val status: SubmissionResponse.Status?,
    val logs: Logs,
)

data class StatusPollingConfiguration(
    val timeout: Duration = 1.hours,
    val pollingPeriod: Duration = 30.seconds,
    /**
     * Whether to ignore 5xx error from Notary API and keep polling status
     */
    val ignoreServerError: Boolean = true,
    val retryDelayAfterServerError: Duration = 5.minutes,
)

/**
 * Issue notarization submission for file and wait for submission to complete
 */
suspend fun NotaryClientV2.notarize(
    filepath: Path,
    pollingConfiguration: StatusPollingConfiguration = StatusPollingConfiguration(),
): NotarizationResult {
    val request = NewSubmissionRequest(
        sha256 = sha256(filepath),
        submissionName = filepath.fileName.toString(),
        notifications = emptyList()
    )
    logger.info("Issuing notarization submission '${request.submissionName}' (${request.sha256})...")
    val submitResponse = submitSoftware(request)
    logger.debug("Submission response:\n${submitResponse}")

    val submissionAttributes = submitResponse.data?.attributes
        ?: error("Apple Notary API response is missing AWS credentials")

    logger.info("Uploading $filepath to Apple S3...")
    logger.debug("Using S3 credentials:\n${submissionAttributes}")
    val uploadResponse = uploadSoftware(submissionAttributes, filepath)
    logger.debug("Response from Apple S3:\n${uploadResponse}")

    val submissionId = submitResponse.data.id
        ?: error("Apple Notary API response is missing ID")
    logger.info("Starting polling status for submission '$submissionId' (with timeout of ${pollingConfiguration.timeout})...")
    val status = withTimeoutOrNull(pollingConfiguration.timeout) {
        awaitSubmissionCompletion(submissionId, pollingConfiguration)
    }
    if (status == null) {
        logger.info("Status polling timed out for submission '$submissionId'")
    } else {
        logger.info("Submission '$submissionId' complete, status: $status")
    }

    logger.info("Requesting logs for submission '$submissionId'...")
    val logs = getSubmissionLog(submissionId)
    val json = Json { prettyPrint = true }
    logger.debug("Logs for submission '$submissionId':\n${json.encodeToString(logs)}")

    return NotarizationResult(
        submissionId = submissionId,
        status = status,
        logs = logs,
    )
}

private suspend fun NotaryClientV2.awaitSubmissionCompletion(
    submissionId: String,
    pollingConfiguration: StatusPollingConfiguration,
): SubmissionResponse.Status {
    while (true) {
        val response = try {
            getSubmissionStatus(submissionId)
        } catch (e: ServerResponseException) {
            if (pollingConfiguration.ignoreServerError) {
                logger.warn("Ignoring call failure to Notary API, will check status again in ${pollingConfiguration.retryDelayAfterServerError}:\n$e")
                delay(pollingConfiguration.retryDelayAfterServerError)
                continue
            } else {
                throw e
            }
        } catch (e: Exception) {
            throw e
        }
        when (val status = response.data?.attributes?.status) {
            SubmissionResponse.Status.ACCEPTED,
            SubmissionResponse.Status.INVALID,
            SubmissionResponse.Status.REJECTED -> return status

            SubmissionResponse.Status.IN_PROGRESS -> logger.info("Notarization still in progress, will check status again in ${pollingConfiguration.pollingPeriod}")
            null -> logger.warn("Notarization status unknown, will check status again in ${pollingConfiguration.pollingPeriod}")
        }
        delay(pollingConfiguration.pollingPeriod)
    }
}

private fun sha256(path: Path): String {
    val bytes = path.readBytes()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.fold("") { str, it -> str + "%02x".format(it) }
}
