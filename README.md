# apple-notary-api-kotlin-client

[![Maven central version](https://img.shields.io/maven-central/v/io.github.archangelx360/notary-api-kotlin-client.svg)](http://mvnrepository.com/artifact/io.github.archangelx360/notary-api-kotlin-client)
[![Github Build](https://github.com/JetBrains/apple-notary-api-kotlin-client/actions/workflows/build.yml/badge.svg)](https://github.com/JetBrains/apple-notary-api-kotlin-client/actions/workflows/build.yml)
[![GitHub license](https://img.shields.io/badge/license-APACHE_2.0-blue.svg)](https://github.com/JetBrains/apple-notary-api-kotlin-client/blob/main/LICENSE)

Apple Notary API client for Kotlin

## Add the dependency

This library is available on Maven Central.

Using Gradle:

```kotlin
implementation("io.github.archangelx360:notary-api-kotlin-client:$version")
```

Using Maven:

```xml

<dependency>
    <groupId>io.github.archangelx360</groupId>
    <artifactId>notary-api-kotlin-client</artifactId>
    <version>$VERSION</version>
</dependency>
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
import io.github.archangelx360.NotaryClientV2
import io.github.archangelx360.auth.AppStoreConnectAPIKey

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

## License

This project is distributed under the Apache 2.0 license.
