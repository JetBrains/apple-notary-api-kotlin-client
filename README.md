# apple-notary-api-kotlin-client

[![JetBrains team project](http://jb.gg/badges/team.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Github Build](https://github.com/JetBrains/apple-notary-api-kotlin-client/actions/workflows/build.yml/badge.svg)](https://github.com/JetBrains/apple-notary-api-kotlin-client/actions/workflows/build.yml)
[![GitHub license](https://img.shields.io/badge/license-APACHE_2.0-blue.svg)](https://github.com/JetBrains/apple-notary-api-kotlin-client/blob/main/LICENSE)

[Apple Notary API](https://developer.apple.com/documentation/notaryapi) client for [Kotlin](https://kotlinlang.org).

Notarize your macOS application from Kotlin code without dependency on [Xcode](https://developer.apple.com/xcode) nor [macOS](https://www.apple.com/macos).

## Add the dependency

This library is available on Space Packages, you will need to add this Maven repository to your Gradle configuration:

```kotlin
import java.net.URI

repositories {
    maven {
        url = URI("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    }
}
```

Then add the dependency

```kotlin
implementation("org.jetbrains:apple-notary-api-kotlin-client:$version")
```

You will also need to provide a Ktor engine implementation, for example if you want to use the CIO engine:

```kotlin
implementation("io.ktor:ktor-client-cio:$ktorVersion")
```

## Usage

You will first need to create an API Key for App Store Connect API,
see [Apple's Documentation](https://developer.apple.com/documentation/appstoreconnectapi/creating_api_keys_for_app_store_connect_api)
.
While doing this process make sure to save:

- your issuer ID from the API Keys page in App Store Connect (e.g. `57246542-96fe-1a63-e053-0824d011072a`)
- your private key ID from App Store Connect (e.g. `2X9R4HXF34`)
- your private key `.p8` file you can download from the API Keys page in App Store Connect

Once you have these information, you can setup a credential object:

```kotlin
import com.jetbrains.notary.NotaryClientV2
import com.jetbrains.notary.auth.AppStoreConnectAPIKey

val credentials = AppStoreConnectAPIKey(
    issuerId = "your-issuer-id",
    keyId = "your-private-key-id",
    privateKey = "your-private-key-file-content",
)

val client = NotaryClientV2(credentials)

// ... now you can use your client!

// For example retrieving your previous submissions:
val submissions = client.getPreviousSubmissions()
println(submissions)
```

### Extensions

`apple-notary-api-kotlin-client` provides some extension functions that cover basic use cases of 
[Apple Notary API](https://developer.apple.com/documentation/notaryapi).

#### Functions `notarize` or `notarizeBlocking`

`notarize` (or its blocking equivalent `notarizeBlocking`) will cover the basic use case of "I want to notarize a file,
wait for the notarization to complete, get status and logs of the submission result when completed".
Under the hood, it will do the following:
- Issue a notarization submission for the specified file
- Upload the specified file to the location asked by [Apple Notary API](https://developer.apple.com/documentation/notaryapi)
- Poll [Apple Notary API](https://developer.apple.com/documentation/notaryapi) until the submission completes (or timeout)
  - _Note: with configuration such as `ignoreServerError` and `ignoreTimeoutExceptions` set to `true` (set by default), polling will be hardened not to fail on usual false negative situations_
- Fetch logs of the completed submission (whether it is a success or a failure)
- Return status and logs of the completed submission

##### Example usage snippet

```kotlin
import com.jetbrains.notary.NotaryClientV2
import com.jetbrains.notary.auth.AppStoreConnectAPIKey
import kotlin.time.Duration.Companion.minutes

val credentials = AppStoreConnectAPIKey(
    issuerId = "your-issuer-id",
    keyId = "your-private-key-id",
    privateKey = "your-private-key-file-content",
)

val notaryApiClient = NotaryClientV2(credentials)
val notarizationResult = notaryApiClient.notarizeBlocking(file, StatusPollingConfiguration(
    timeout = 30.minutes,
    pollingPeriod = 1.minutes,
    ignoreServerError = true,
    ignoreTimeoutExceptions = true,
))

val json = Json { prettyPrint = true }
println("Notarization logs:\n${json.encodeToString(notarizationResult.logs)}")

when (notarizationResult.status) {
    SubmissionResponse.Status.ACCEPTED -> println("Notarization of $file successful")
    SubmissionResponse.Status.IN_PROGRESS, null -> error("Timed out polling status of $file notarization submission")
    SubmissionResponse.Status.INVALID, SubmissionResponse.Status.REJECTED -> error("Notarization of $file failed, see logs above")
}
```

### Configuration

#### Use your own `ktor` client implementation

You are free to use your own `ktor` client implementation and thus configuration.
We provide `NotaryClientV2.defaultHttpClient` to allow your client configuration to extend the default one.

##### Example of configuration: change retry policy

```kotlin
import com.jetbrains.notary.NotaryClientV2
import com.jetbrains.notary.auth.AppStoreConnectAPIKey
import kotlin.time.Duration.Companion.minutes

val credentials = AppStoreConnectAPIKey(
  issuerId = "your-issuer-id",
  keyId = "your-private-key-id",
  privateKey = "your-private-key-file-content",
)

val myOwnClient = NotaryClientV2.defaultHttpClient.config { // keep all the default configuration
  install(HttpRequestRetry) {
    retryOnExceptionOrServerErrors(maxRetries = 10) // but increase the number of retry of the default configuration
    constantDelay(millis = 1.minutes.inWholeMilliseconds) // but change the default exponential retry by a constant one
  }
}

val notaryApiClient = NotaryClientV2(credentials, httpClient = myOwnClient)
```

## License

This project is distributed under the Apache 2.0 license.

## Acknowledgment

Special thanks to the people that built Rust library
[apple-codesign](https://github.com/indygreg/apple-platform-rs/tree/main/apple-codesign) that inspired this work.
