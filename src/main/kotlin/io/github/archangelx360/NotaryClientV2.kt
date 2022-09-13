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
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import java.nio.file.Path

class NotaryClientV2(
    private val credentials: AppStoreConnectAPIKey,
    private val baseUrl: String = "https://appstoreconnect.apple.com",
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json()
            // Apple does not respect Accept header, so we work around to make Ktor still deserialize `application/octet-stream` as JSON
            serialization(ContentType.Application.OctetStream, DefaultJson)
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }
    },
) {
    /**
     * Start the process of uploading a new version of your software to the notary service.
     *
     * https://developer.apple.com/documentation/notaryapi/submit_software
     */
    suspend fun submitSoftware(request: NewSubmissionRequest): NewSubmissionResponse {
        val response = httpClient.post("$baseUrl/notary/v2/submissions") {
            withAppleAuthentication(credentials)
            setBody(request)
            expectSuccess = true
        }
        return response.body()
    }

    /**
     * Fetch a list of your team’s previous notarization submissions.
     *
     * https://developer.apple.com/documentation/notaryapi/get_previous_submissions
     */
    suspend fun getPreviousSubmissions(): SubmissionListResponse {
        val response = httpClient.get("$baseUrl/notary/v2/submissions") {
            withAppleAuthentication(credentials)
            expectSuccess = true
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
        filepath: Path,
        // The region is not given anywhere in the Apple documentation.
        // Thanks to the folks that built https://github.com/indygreg/PyOxidizer, we got it's us-west-2...
        s3Region: String = "us-west-2",
    ): PutObjectResponse {
        val request = PutObjectRequest {
            bucket = attributes.bucket
            key = attributes.`object`
            metadata = emptyMap()
            body = filepath.asByteStream()
        }

        val credentials = StaticCredentialsProvider {
            accessKeyId = attributes.awsAccessKeyId
            secretAccessKey = attributes.awsSecretAccessKey
            sessionToken = attributes.awsSessionToken
        }

        return S3Client {
            region = s3Region
            credentialsProvider = credentials
        }.use { s3 ->
            s3.putObject(request)
        }
    }

    /**
     * Fetch the status of a software notarization submission.
     *
     * https://developer.apple.com/documentation/notaryapi/get_submission_status
     */
    suspend fun getSubmissionStatus(submissionId: String): SubmissionResponse {
        val response = httpClient.get("$baseUrl/notary/v2/submissions/$submissionId") {
            withAppleAuthentication(credentials)
            expectSuccess = true
        }
        return response.body()
    }

    /**
     * Fetch details about a single completed notarization.
     *
     * https://developer.apple.com/documentation/notaryapi/get_submission_log
     */
    suspend fun getSubmissionLog(submissionId: String): Logs {
        val response = httpClient.get("$baseUrl/notary/v2/submissions/$submissionId/logs") {
            withAppleAuthentication(credentials)
            expectSuccess = true
        }
        val body = response.body<SubmissionLogURLResponse>()
        val url = body.data?.attributes?.developerLogUrl
            ?: error("developerLogUrl is missing from response attribute")
        val logReponse = httpClient.get(url) {
            expectSuccess = true
        }
        return logReponse.body()
    }
}
