package com.jetbrains.notary.models

import com.jetbrains.notary.notaryClientJson
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class LogsTest {
    @Test
    fun shouldDeserializeLogsV1() {
        val json = readResourceFile("/v1_logs_without_issues.json")
        val logs = notaryClientJson.decodeFromString<Logs>(json)
        assert(logs is Logs.V1)
    }

    @Test
    fun shouldDeserializeLogsV1ContainingIssues() {
        val json = readResourceFile("/v1_logs_with_issues.json")
        val logs = notaryClientJson.decodeFromString<Logs>(json)
        assert(logs is Logs.V1)
    }

    @Test
    fun failSerializationOfUnsupportedLogVersion() {
        assertThrows<SerializationException> {
            val json = readResourceFile("/v2_logs.json")
            notaryClientJson.decodeFromString<Logs>(json)
        }
    }

    @Test
    fun shouldDeserializeLogsV1OfDmg() {
        val json = readResourceFile("/v1_logs_dmg.json")
        val logs = notaryClientJson.decodeFromString<Logs>(json)
        assert(logs is Logs.V1)
    }

    private fun readResourceFile(filepath: String) =
        LogsTest::class.java.getResource(filepath)?.readText() ?: error("failed to read resource file $filepath")
}
