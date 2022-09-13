package io.github.archangelx360.models

/**
 * The notary service’s response when an error occurs.
 *
 * https://developer.apple.com/documentation/notaryapi/errorresponse
 */
data class ErrorResponse(
    /**
     * The name of the error.
     */
    val name: String?,
    /**
     * A string that describes the reason for the error.
     */
    val description: String?,
    /**
     * Additional information about the error.
     */
    val label: List<String>?,
)
