package com.example

import com.example.util.PromptPayHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptPayHelperTest {

    @Test
    fun buildPayload_matchesPromptPayEmvFormat() {
        val payload = PromptPayHelper.buildPayload("081-234-5678", 50.0)
        assertEquals(
            "00020101021229370016A000000677010111011300668123456785303764540550.005802TH63045197",
            payload
        )
    }

    @Test
    fun buildPayload_supportsNationalId() {
        val payload = PromptPayHelper.buildPayload("1234567890123", 0.0)
        assertTrue(payload.contains("29370016A00000067701011102131234567890123"))
        assertTrue(payload.contains("53037645802TH"))
        assertEquals(4, payload.takeLast(4).length)
    }

    @Test
    fun generateQrMatrix_returnsSquareMatrix() {
        val payload = PromptPayHelper.buildPayload("0812345678", 120.0)
        val matrix = PromptPayHelper.generateQrMatrix(payload)
        assertTrue(matrix.isNotEmpty())
        assertTrue(matrix.all { it.size == matrix.size })
    }
}
