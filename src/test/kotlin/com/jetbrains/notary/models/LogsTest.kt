package com.jetbrains.notary.models

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class LogsTest {
    @Test
    fun shouldDeserializeLogsV1() {
        val json = readResourceFile("/v1_logs_without_issues.json")
        val logs = Json.decodeFromString<Logs>(json)
        assert(logs is Logs.V1)
    }

    @Test
    fun shouldDeserializeLogsV1ContainingIssues() {
        val json = readResourceFile("/v1_logs_with_issues.json")
        val logs = Json.decodeFromString<Logs>(json)
        assert(logs is Logs.V1)
    }

    @Test
    fun failSerializationOfUnsupportedLogVersion() {
        assertThrows<SerializationException> {
            val json = readResourceFile("/v2_logs.json")
            Json.decodeFromString<Logs>(json)
        }
    }

    private fun readResourceFile(filepath: String) = LogsTest::class.java.getResource(filepath)?.readText()
        ?: error("failed to read resource file $filepath")
}
