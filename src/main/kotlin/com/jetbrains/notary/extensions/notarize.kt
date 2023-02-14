package com.jetbrains.notary.extensions

import com.jetbrains.notary.NotaryClientV2
import com.jetbrains.notary.models.Logs
import com.jetbrains.notary.models.NewSubmissionRequest
import com.jetbrains.notary.models.SubmissionResponse
import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.security.DigestInputStream
import java.security.MessageDigest
import kotlin.io.path.extension
import kotlin.io.path.inputStream
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
     * Whether to ignore 5xx error from Notary API and keep polling status instead of throwing
     */
    val ignoreServerError: Boolean = true,
    /**
     * Whether to ignore timeout exception (HttpRequestTimeoutException, SocketTimeoutException,
     * ConnectTimeoutException) and keep polling status instead of throwing
     */
    val ignoreTimeoutExceptions: Boolean = true,
    /**
     * Delay to wait after a failure has been ignored (timeout or server error).
     *
     * After a failure is ignored we will not respect the `pollingPeriod` duration but instead the
     * `retryDelayAfterFailure` one, to avoid bursting the Notary API server while it is clearly having issues.
     */
    val retryDelayAfterFailure: Duration = 5.minutes,
)

/**
 * Issue notarization submission for specified file and wait for submission to complete
 *
 * @param filepath path to file to notarize
 * @param pollingConfiguration configuration for status polling strategy
 */
fun NotaryClientV2.notarizeBlocking(
    filepath: Path,
    pollingConfiguration: StatusPollingConfiguration = StatusPollingConfiguration(),
): NotarizationResult = runBlocking { notarize(filepath, pollingConfiguration) }

/**
 * Issue notarization submission for specified file and wait for submission to complete
 *
 * @param filepath path to file to notarize, only .zip and .dmg are supported
 * @param pollingConfiguration configuration for status polling strategy
 */
suspend fun NotaryClientV2.notarize(
    filepath: Path,
    pollingConfiguration: StatusPollingConfiguration = StatusPollingConfiguration(),
): NotarizationResult {
    require(filepath.extension == "zip" || filepath.extension == "dmg") {
        "$filepath: only .zip and .dmg files can be notarized"
    }
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
        } catch (e: Exception) {
            when {
                (pollingConfiguration.ignoreServerError && e is ServerResponseException) ->
                    logger.warn("Ignoring call failure to Notary API, will check status again in ${pollingConfiguration.retryDelayAfterFailure}:\n$e")

                (pollingConfiguration.ignoreTimeoutExceptions && e.isTimeoutException()) ->
                    logger.warn("Ignoring call timeout to Notary API, will check status again in ${pollingConfiguration.retryDelayAfterFailure}:\n$e")

                else -> throw e
            }
            delay(pollingConfiguration.retryDelayAfterFailure)
            continue
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

private fun Exception.isTimeoutException(): Boolean = this is HttpRequestTimeoutException
        || this is SocketTimeoutException
        || this is ConnectTimeoutException

private fun sha256(path: Path): String {
    val md = MessageDigest.getInstance("SHA-256")
    DigestInputStream(path.inputStream().buffered(), md).use {
        val buffer = ByteArray(1024)
        while (true) {
            val readCount: Int = it.read(buffer)
            if (readCount < 0) {
                break
            }
        }
    }
    return md.digest().fold("") { str, it -> str + "%02x".format(it) }
}
