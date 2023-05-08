package com.jetbrains.notary

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicSessionCredentials
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.model.PutObjectResult
import com.jetbrains.notary.auth.AppStoreConnectAPIKey
import com.jetbrains.notary.auth.withAppleAuthentication
import com.jetbrains.notary.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalSerializationApi::class)
internal val notaryClientJson = Json {
    explicitNulls = false
}

class NotaryClientV2(
    private val credentials: AppStoreConnectAPIKey,
    private val baseUrl: String = "https://appstoreconnect.apple.com",
    private val httpClient: HttpClient = defaultHttpClient,
) {
    companion object {
        val defaultHttpClient = HttpClient {
            install(ContentNegotiation) {
                json(notaryClientJson)
                // Apple does not respect Accept header, so we work around to make Ktor still deserialize `application/octet-stream` as JSON
                serialization(ContentType.Application.OctetStream, DefaultJson)
            }
            install(HttpRequestRetry) {
                retryOnExceptionOrServerErrors(maxRetries = 4)
                exponentialDelay(
                    base = 5.0, // 5s, 25s, 125s, etc.
                    maxDelayMs = 10.minutes.inWholeMilliseconds,
                )
            }
        }
    }

    /**
     * Start the process of uploading a new version of your software to the notary service.
     *
     * https://developer.apple.com/documentation/notaryapi/submit_software
     */
    suspend fun submitSoftware(request: NewSubmissionRequest): NewSubmissionResponse {
        val response = httpClient.post("$baseUrl/notary/v2/submissions") {
            withAppleAuthentication(credentials)
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
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
            accept(ContentType.Application.Json)
            expectSuccess = true
        }
        return response.body()
    }

    /**
     * Upload file to Apple S3 storage.
     *
     * This is meant to be used after submitting a request using `submitSoftware`.
     */
    fun uploadSoftware(
        attributes: NewSubmissionResponse.Data.Attributes,
        filepath: Path,
        // The region is not given anywhere in the Apple documentation.
        // Thanks to the folks that built https://github.com/indygreg/PyOxidizer, we got it's us-west-2...
        s3Region: String = "us-west-2",
    ): PutObjectResult? {
        val inputStream = filepath.inputStream().buffered()
        val metadata = ObjectMetadata().also {
            it.contentLength = filepath.fileSize()
        }
        val request = PutObjectRequest(
            attributes.bucket,
            attributes.`object`,
            inputStream,
            metadata,
        )
        val credentials = BasicSessionCredentials(
            attributes.awsAccessKeyId,
            attributes.awsSecretAccessKey,
            attributes.awsSessionToken,
        )
        val s3Client = AmazonS3ClientBuilder
            .standard()
            .withCredentials(AWSStaticCredentialsProvider(credentials))
            .withRegion(s3Region)
            .build()
        return s3Client.putObject(request)
    }

    /**
     * Fetch the status of a software notarization submission.
     *
     * https://developer.apple.com/documentation/notaryapi/get_submission_status
     */
    suspend fun getSubmissionStatus(submissionId: String): SubmissionResponse {
        val response = httpClient.get("$baseUrl/notary/v2/submissions/$submissionId") {
            withAppleAuthentication(credentials)
            accept(ContentType.Application.Json)
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
            accept(ContentType.Application.Json)
            expectSuccess = true
        }
        val body = response.body<SubmissionLogURLResponse>()
        val url = body.data?.attributes?.developerLogUrl
            ?: error("developerLogUrl is missing from response attribute")
        val logReponse = httpClient.get(url) {
            expectSuccess = true
        }
        // We consume the response as text other we are getting
        // ```
        // io.ktor.client.call.NoTransformationFoundException: No transformation found:
        // class io.ktor.utils.io.ByteBufferChannel (Kotlin reflection is not available)
        // -> class com.jetbrains.notary.models.Logs (Kotlin reflection is not available)
        // ```
        // Probably due to an invalid response Content-Type.
        val logs = logReponse.bodyAsText()
        return Json.decodeFromString(logs)
    }
}
