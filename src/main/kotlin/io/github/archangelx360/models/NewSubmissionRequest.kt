package io.github.archangelx360.models

import kotlinx.serialization.Serializable

/**
 * Data that you provide when starting a submission to the notary service.
 *
 * https://developer.apple.com/documentation/notaryapi/newsubmissionrequest
 */
@Serializable
data class NewSubmissionRequest(
    /**
     * A cryptographic hash of the software that you want to notarize, computed using Secure Hashing Algorithm 2 (SHA-2) with a 256-bit digest.
     * Supply the hash as a string of 64 hexadecimal digits.
     * You must compute the hash from the exact version of the software that you plan to upload to Amazon S3.
     *
     * `Value: /[A-Fa-f0-9]{64}/`
     */
    val sha256: String,
    /**
     * The name of the file that you plan to submit.
     * The service includes this name in its responses when you ask for the status of a submission, get a list of previous submissions, or get a log file corresponding to a submission.
     * The file name doesn’t have to be unique among all your submissions, but making it so might help you to distinguish among submissions in service responses.
     */
    val submissionName: String,
    /**
     * An optional array of notifications that you want to receive when notarization finishes.
     * Omit this key if you don’t need a notification.
     */
    val notifications: List<Notifications>,
) {
    /**
     * A notification that the notary service sends you when notarization finishes.
     *
     * https://developer.apple.com/documentation/notaryapi/newsubmissionrequest/notifications
     */
    @Serializable
    data class Notifications(
        /**
         * The channel that the service uses to notify you when notarization completes. The only supported value for this key is webhook.
         */
        val channel: String?,
        /**
         * The URL that the notary service accesses when notarization completes.
         */
        val target: String?,
    )
}