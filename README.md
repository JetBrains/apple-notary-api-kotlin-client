# notary-api-kotlin-client

[![Maven central version](https://img.shields.io/maven-central/v/io.github.archangelx360/notary-api-kotlin-client.svg)](http://mvnrepository.com/artifact/io.github.archangelx360/notary-api-kotlin-client)
[![Github Build](https://github.com/ArchangelX360/notary-api-kotlin-client/actions/workflows/build.yml/badge.svg)](https://github.com/ArchangelX360/notary-api-kotlin-client/actions/workflows/build.yml)
[![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/ArchangelX360/notary-api-kotlin-client/blob/main/LICENSE)

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

## License

This project is distributed under the MIT license.
