package io.github.archangelx360.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The notary service’s response to a request for information about your team’s previous submissions.
 *
 * https://developer.apple.com/documentation/notaryapi/submissionlistresponse
 */
@Serializable
data class SubmissionListResponse(
    val data: List<Data>,
    val meta: Meta,
) {
    /**
     * Data that describes one of your team’s previous submissions.
     *
     * https://developer.apple.com/documentation/notaryapi/submissionlistresponse/data
     */
    @Serializable
    data class Data(
        /**
         * Information about a particular submission.
         */
        val attributes: Attributes,
        /**
         * The unique identifier for a submission.
         * This value matches the value that you received in the id field that appeared in the response to the Submit Software call that you used to start the submission.
         */
        val id: String,
        /**
         * The resource type.
         */
        val type: String,
    ) {
        /**
         * Information about the status of a submission.
         *
         * https://developer.apple.com/documentation/notaryapi/submissionlistresponse/data/attributes
         */
        @Serializable
        data class Attributes(
            /**
             * The date that you started the submission process, given in ISO 8601 format, like 2022-06-08T01:38:09.498Z.
             */
            val createdDate: String,
            /**
             * The name that you specified in the submissionName field of the Submit Software call when you started the submission.
             */
            val name: String,
            /**
             * The status of the submission.
             */
            val status: Status,
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

    @Serializable
    class Meta
}
