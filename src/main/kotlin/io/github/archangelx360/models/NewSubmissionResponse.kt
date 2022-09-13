package io.github.archangelx360.models

import kotlinx.serialization.Serializable

/**
 * The notary service’s response to a software submission.
 *
 * https://developer.apple.com/documentation/notaryapi/submit_software
 */
@Serializable
data class NewSubmissionResponse(
    /**
     * Data that describes the result of the submission request.
     */
    val data: Data?,
    /**
     * An empty object that you can ignore.
     */
    val meta: Meta?,
) {
    /**
     * Information that the notary service provides for uploading your software for notarization and tracking the submission.
     *
     * https://developer.apple.com/documentation/notaryapi/newsubmissionresponse/data
     */
    @Serializable
    data class Data(
        /**
         * A unique identifier for this submission. Use this value to track the status of your submission.
         * For example, you use it as the submissionID parameter in the Get Submission Status call, or to match against the id field in the response from the Get Previous Submissions call.
         */
        val id: String?,
        val type: String?,
        /**
         * Information that you use to upload your software to Amazon S3.
         */
        val attributes: Attributes?
    ) {
        /**
         * Information that you use to upload your software for notarization.
         *
         * https://developer.apple.com/documentation/notaryapi/newsubmissionresponse/data/attributes
         */
        @Serializable
        data class Attributes(
            /**
             * An access key that you use in a call to Amazon S3.
             */
            val awsAccessKeyId: String?,
            /**
             * A secret key that you use in a call to Amazon S3.
             */
            val awsSecretAccessKey: String?,
            /**
             * A session token that you use in a call to Amazon S3.
             */
            val awsSessionToken: String?,
            /**
             * The Amazon S3 bucket that you upload your software into.
             */
            val bucket: String?,
            /**
             * The object key that identifies your software upload within the bucket.
             */
            val `object`: String?,
        )
    }

    /**
     * An empty object.
     *
     * https://developer.apple.com/documentation/notaryapi/newsubmissionresponse/meta
     */
    @Serializable
    class Meta
}
