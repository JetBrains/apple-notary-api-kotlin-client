package io.github.archangelx360.extensions

import io.github.archangelx360.NotaryClientV2
import io.github.archangelx360.models.Logs
import io.github.archangelx360.models.NewSubmissionRequest
import io.github.archangelx360.models.SubmissionResponse
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
import kotlin.time.Duration.Companion.seconds

private val logger = LoggerFactory.getLogger("notary-api-kotlin-client")

data class NotarizationResult(
    val submissionId: String,
    val status: SubmissionResponse.Status?,
    val logs: Logs,
)

suspend fun NotaryClientV2.notarize(
    filepath: Path,
    timeout: Duration = 1.hours,
    pollingFrequency: Duration = 30.seconds,
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
    logger.info("Starting polling status for submission '$submissionId'...")
    val status = withTimeoutOrNull(timeout) {
        awaitSubmissionCompletion(submissionId, pollingFrequency)
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
    pollingPeriod: Duration,
): SubmissionResponse.Status {
    while (true) {
        val response = getSubmissionStatus(submissionId)
        when (val status = response.data?.attributes?.status) {
            SubmissionResponse.Status.ACCEPTED,
            SubmissionResponse.Status.INVALID,
            SubmissionResponse.Status.REJECTED -> return status

            SubmissionResponse.Status.IN_PROGRESS -> logger.info("Notarization still in progress, will check status again in $pollingPeriod")
            null -> logger.warn("Notarization status unknown, will check status again in $pollingPeriod")
        }
        delay(pollingPeriod)
    }
}

private fun sha256(path: Path): String {
    val bytes = path.readBytes()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.fold("") { str, it -> str + "%02x".format(it) }
}
