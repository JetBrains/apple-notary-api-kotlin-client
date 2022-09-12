package io.github.archangelx360

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectResponse
import aws.smithy.kotlin.runtime.content.asByteStream
import io.github.archangelx360.auth.AppStoreConnectAPIKey
import io.github.archangelx360.auth.withAppleAuthentication
import io.github.archangelx360.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.io.File
import java.util.*

class NotaryClientV2(
    private val credentials: AppStoreConnectAPIKey,
    private val httpClient: HttpClient = HttpClient {},
    private val baseUrl: String = "https://appstoreconnect.apple.com/notary/v2"
) {
    /**
     * Start the process of uploading a new version of your software to the notary service.
     *
     * https://developer.apple.com/documentation/notaryapi/submit_software
     */
    suspend fun submitSoftware(request: NewSubmissionRequest): NewSubmissionResponse {
        val response = httpClient.post("$baseUrl/submissions") {
            withAppleAuthentication(credentials)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.body()
    }

    /**
     * Upload file to Apple S3 storage.
     *
     * This is meant to be used after submitting a request using `submitSoftware`.
     */
    suspend fun uploadSoftware(
        attributes: NewSubmissionResponse.Data.Attributes,
        file: File,
        s3Region: String = "us-east-1"
    ): PutObjectResponse {
        val request = PutObjectRequest {
            bucket = attributes.bucket
            key = attributes.`object`
            metadata = emptyMap()
            body = file.asByteStream()
        }

        val credentials = StaticCredentialsProvider {
            accessKeyId = attributes.awsAccessKeyId
            secretAccessKey = attributes.awsSecretAccessKey
            sessionToken = attributes.awsSessionToken
        }

        S3Client {
            region = s3Region
            credentialsProvider = credentials
        }.use { s3 ->
            return s3.putObject(request)
        }
    }

    /**
     * Fetch the status of a software notarization submission.
     *
     * https://developer.apple.com/documentation/notaryapi/get_submission_status
     */
    suspend fun getSubmissionStatus(submissionId: UUID): NotaryResponse<SubmissionResponse> {
        val response = httpClient.get("$baseUrl/submissions/$submissionId") {
            withAppleAuthentication(credentials)
        }

        return when (response.status) {
            HttpStatusCode.OK -> {
                val body = response.body<SubmissionResponse>()
                NotaryResponse.Success(body)
            }

            HttpStatusCode.Forbidden, HttpStatusCode.NotFound -> {
                val body = response.body<ErrorResponse>()
                NotaryResponse.Error(body)
            }

            else -> error("unsupported status code ${response.status}: ${response.body<String>()}")
        }
    }

    /**
     * Fetch details about a single completed notarization.
     *
     * https://developer.apple.com/documentation/notaryapi/get_submission_log
     */
    suspend fun getSubmissionLog(submissionId: UUID): NotaryResponse<Logs> {
        val response = httpClient.get("$baseUrl/submissions/$submissionId/logs") {
            withAppleAuthentication(credentials)
        }

        return when (response.status) {
            HttpStatusCode.OK -> {
                val body = response.body<SubmissionLogURLResponse>()
                val url = body.data.attributes.developerLogUrl
                val logReponse = httpClient.get(url)
                val logs = logReponse.body<Logs>()
                NotaryResponse.Success(logs)
            }

            HttpStatusCode.Forbidden, HttpStatusCode.NotFound -> {
                val body = response.body<ErrorResponse>()
                NotaryResponse.Error(body)
            }

            else -> error("unsupported status code ${response.status}: ${response.body<String>()}")
        }
    }
}

sealed class NotaryResponse<T> {
    class Success<T>(val response: T) : NotaryResponse<T>()
    class Error<T>(val response: ErrorResponse) : NotaryResponse<T>()
}
