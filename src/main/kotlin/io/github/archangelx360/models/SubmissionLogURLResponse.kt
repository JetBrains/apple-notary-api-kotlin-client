package io.github.archangelx360.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * The notary service’s response to a request for the log information about a completed submission.
 *
 * https://developer.apple.com/documentation/notaryapi/submissionlogurlresponse
 */
@Serializable
data class SubmissionLogURLResponse(
    /**
     * Data that indicates how to get the log information for a particular submission.
     */
    val data: Data?,
    /**
     * An empty object that you can ignore.
     */
    val meta: Meta?,
) {
    @Serializable
    data class Data(
        /**
         * The unique identifier for this submission. This value matches the value that you provided as a path parameter to the Get Submission Log call that elicited this response.
         */
        val id: String?,
        /**
         * The resource type.
         */
        val type: String?,
        /**
         *
         */
        val attributes: Attributes?,
    ) {
        /**
         * Information about the log associated with the submission.
         *
         * https://developer.apple.com/documentation/notaryapi/submissionlogurlresponse/data/attributes
         */
        @Serializable
        data class Attributes(
            /**
             * The URL that you use to download the logs for a submission. The URL serves a JSON-encoded file that contains the log information.
             * The URL is valid for only a few hours. If you need the log again later, ask for the URL again by making another call to the Get Submission Log endpoint.
             */
            val developerLogUrl: String?,
        )
    }

    /**
     * An empty object.
     *
     * https://developer.apple.com/documentation/notaryapi/submissionlogurlresponse/meta
     */
    @Serializable
    class Meta
}

@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("logFormatVersion")
sealed class Logs {
    @Serializable
    @SerialName("1")
    class V1(
        val logFormatVersion: Int,
        val jobId: String?,
        val status: SubmissionResponse.Status?,
        val statusSummary: String?,
        val statusCode: Int?,
        val archiveFilename: String?,
        val uploadDate: String?,
        val sha256: String?,
        val ticketContents: List<TicketContent>? = emptyList(),
        val issues: List<Issue>? = emptyList(),
    ) : Logs() {
        @Serializable
        data class TicketContent(
            val path: String?,
            val digestAlgorithm: String?,
            val cdhash: String?,
            val arch: String?,
        )

        @Serializable
        data class Issue(
            val severity: String?,
            val code: String?,
            val path: String?,
            val message: String?,
            val docUrl: String?,
            val architecture: String?,
        )
    }
}
