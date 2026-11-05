package com.example.util

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.Locale

object PromptPayHelper {

    private const val PROMPTPAY_AID = "A000000677010111"

    /**
     * Formats PromptPay phone number (e.g. 0812345678 -> 081-234-5678)
     */
    fun formatPhoneNumber(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.length == 10 -> "${digits.substring(0, 3)}-${digits.substring(3, 6)}-${digits.substring(6)}"
            digits.length == 9 -> "${digits.substring(0, 2)}-${digits.substring(2, 5)}-${digits.substring(5)}"
            else -> raw
        }
    }

    /**
     * Builds an EMVCo / PromptPay QR payload (tag 29) with an optional amount.
     * Supports mobile numbers (10 digits), national ID (13 digits) and e-Wallet ID (15 digits).
     */
    fun buildPayload(rawPromptPayId: String, amount: Double): String {
        val digits = rawPromptPayId.filter { it.isDigit() }
        val merchantAccount = when {
            digits.length == 13 -> field("02", digits)
            digits.length == 15 -> field("03", digits)
            digits.length == 10 && digits.startsWith("0") ->
                field("01", "0066" + digits.substring(1))
            digits.length == 9 -> field("01", "0066" + digits)
            else -> throw IllegalArgumentException("หมายเลขพร้อมเพย์ไม่ถูกต้อง")
        }

        val promptPayTag = field("00", PROMPTPAY_AID) + merchantAccount
        val hasAmount = amount > 0.0

        val payload = buildString {
            append(field("00", "01")) // Payload Format Indicator
            append(field("01", if (hasAmount) "12" else "11")) // Dynamic / static QR
            append(field("29", promptPayTag))
            append(field("53", "764")) // THB
            if (hasAmount) {
                append(field("54", String.format(Locale.US, "%.2f", amount)))
            }
            append(field("58", "TH"))
            append("6304") // CRC tag + length, CRC value appended below
        }

        return payload + crc16(payload.toByteArray(Charsets.UTF_8))
    }

    fun generateQrMatrix(data: String): Array<BooleanArray> {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1
        )
        // A width/height of 0 keeps the matrix at module resolution, which is
        // exactly what we need for drawing on a Compose Canvas.
        val matrix = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, 0, 0, hints)
        return Array(matrix.height) { y ->
            BooleanArray(matrix.width) { x -> matrix.get(x, y) }
        }
    }

    private fun field(id: String, value: String): String =
        id + value.length.toString().padStart(2, '0') + value

    private fun crc16(data: ByteArray): String {
        var crc = 0xFFFF
        for (byte in data) {
            crc = crc xor ((byte.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) {
                    (crc shl 1) xor 0x1021
                } else {
                    crc shl 1
                }
                crc = crc and 0xFFFF
            }
        }
        return String.format(Locale.US, "%04X", crc)
    }
}

@Composable
fun PromptPayQrView(
    promptPayNumber: String,
    amount: Double,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 200.dp
) {
    val payload = remember(promptPayNumber, amount) {
        runCatching { PromptPayHelper.buildPayload(promptPayNumber, amount) }.getOrNull()
    }
    val matrix = remember(payload) {
        payload?.let { runCatching { PromptPayHelper.generateQrMatrix(it) }.getOrNull() }
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(2.dp, Color(0xFF1E3A8A), RoundedCornerShape(12.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (matrix != null) {
            Canvas(modifier = Modifier.size(sizeDp - 24.dp)) {
                val cellSize = this.size.width / matrix.size
                for (row in matrix.indices) {
                    for (column in matrix[row].indices) {
                        if (matrix[row][column]) {
                            drawRect(
                                color = Color(0xFF111827),
                                topLeft = Offset(column * cellSize, row * cellSize),
                                size = Size(cellSize, cellSize)
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                text = "ไม่สามารถสร้าง QR Code ได้\nกรุณาตรวจสอบเบอร์พร้อมเพย์ในหน้าตั้งค่า",
                color = Color(0xFFB91C1C),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
