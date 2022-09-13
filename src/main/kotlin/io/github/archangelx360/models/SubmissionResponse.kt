package io.github.archangelx360.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The notary service’s response to a request for the status of a submission.
 *
 * https://developer.apple.com/documentation/notaryapi/submissionresponse
 */
@Serializable
data class SubmissionResponse(
    /**
     * Data that describes the status of the submission request.
     */
    val data: Data?,
    /**
     * An empty object that you can ignore.
     */
    val meta: Meta?,
) {
    /**
     * Information that the service provides about the status of a notarization submission.
     *
     * https://developer.apple.com/documentation/notaryapi/submissionresponse/data
     */
    @Serializable
    data class Data(
        /**
         * The unique identifier for this submission. This value matches the value that you provided as a path parameter to the Get Submission Status call that elicited this response.
         */
        val id: String?,
        /**
         * The resource type.
         */
        val type: String?,
        /**
         * Information about the status of a submission.
         */
        val attributes: Attributes?,
    ) {
        /**
         * Information about the status of a submission.
         *
         * https://developer.apple.com/documentation/notaryapi/submissionresponse/data/attributes
         */
        @Serializable
        data class Attributes(
            /**
             * The date that you started the submission process, given in ISO 8601 format, like 2022-06-08T01:38:09.498Z.
             */
            val createdDate: String?,
            /**
             * The name that you specified in the submissionName field of the Submit Software call when you started the submission.
             */
            val name: String?,
            /**
             * The status of the submission.
             */
            val status: Status?,
        )
    }

    @Serializable
    enum class Status {
        @SerialName("Accepted")
        ACCEPTED,

        @SerialName("In Progress")
        IN_PROGRESS,

        @SerialName("Invalid")
        INVALID,

        @SerialName("Rejected")
        REJECTED,
    }

    /**
     * An empty object.
     *
     * https://developer.apple.com/documentation/notaryapi/submissionresponse/meta
     */
    @Serializable
    class Meta
}
