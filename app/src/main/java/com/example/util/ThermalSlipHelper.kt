package com.example.util

import com.example.data.model.Order
import com.example.data.model.ShopInfo
import java.util.Locale

object ThermalSlipHelper {

    fun generateSlipText(shopInfo: ShopInfo, order: Order): String {
        val sb = StringBuilder()
        val line = "--------------------------------"
        val doubleLine = "================================"

        sb.appendLine("        ${shopInfo.name}        ")
        sb.appendLine("   ${shopInfo.slogan}   ")
        sb.appendLine(shopInfo.address)
        sb.appendLine("โทร/พร้อมเพย์: ${shopInfo.promptPayNumber}")
        sb.appendLine(doubleLine)
        sb.appendLine("ใบเสร็จรับเงิน / ใบสั่งอาหาร")
        sb.appendLine("เลขออเดอร์ : #${order.id}")
        sb.appendLine("เวลา       : ${order.timestamp.ifEmpty { "ปัจจุบัน" }}")
        sb.appendLine("ลูกค้า     : ${if (order.isGuest) "ลูกค้าทั่วไป (Guest)" else order.customerPhone}")
        sb.appendLine("การรับสินค้า: ${order.fulfillment.title}")
        if (order.deliveryAddress.isNotEmpty()) {
            sb.appendLine("ที่อยู่     : ${order.deliveryAddress}")
        }
        sb.appendLine(line)
        sb.appendLine("รายการ                    จำนวน  ราคา")
        sb.appendLine(line)

        order.items.forEach { item ->
            val name = if (item.name.length > 14) item.name.substring(0, 14) else item.name.padEnd(14)
            val qty = item.quantity.toString().padStart(3)
            val price = String.format(Locale.US, "%.0f", item.price * item.quantity).padStart(6)
            sb.appendLine("$name $qty $price.-")
        }

        sb.appendLine(line)
        sb.appendLine("ตัวเลือกเสริม:")
        sb.appendLine("- ไม้เสียบ : ${order.options.skewerLabel.ifBlank { order.options.skewer.displayName }}")
        val condiments = mutableListOf<String>()
        if (order.options.friedChili) condiments.add("พริกทอด")
        if (order.options.cucumber) condiments.add("แตงกวา")
        if (order.options.sauceCup) condiments.add("ถ้วยน้ำจิ้ม")
        sb.appendLine("- ผัก/เคียง : ${if (condiments.isEmpty()) "ไม่รับ" else condiments.joinToString(", ")}")
        sb.appendLine("- น้ำจิ้ม   : ${order.options.sauceChoice.displayName}")
        if (order.comment.isNotEmpty()) {
            sb.appendLine("หมายเหตุ : ** ${order.comment} **")
        }

        sb.appendLine(line)
        sb.appendLine("ยอดรวมสินค้า : ${String.format(Locale.US, "%.2f", order.subtotal)} บาท")
        if (order.discount > 0) {
            sb.appendLine("ส่วนลดโปรโมชั่น : -${String.format(Locale.US, "%.2f", order.discount)} บาท")
        }
        if (order.deliveryFee > 0) {
            sb.appendLine("ค่าจัดส่ง       : ${String.format(Locale.US, "%.2f", order.deliveryFee)} บาท")
        }
        sb.appendLine(doubleLine)
        sb.appendLine("ยอดสุทธิชำระ : ${String.format(Locale.US, "%.2f", order.totalAmount)} บาท")
        sb.appendLine(doubleLine)
        sb.appendLine("    ขอบคุณที่อุดหนุนครับ/ค่ะ!    ")
        sb.appendLine("     (พิมพ์โดยระบบร้านนายเอ็ด)     ")
        return sb.toString()
    }
}
