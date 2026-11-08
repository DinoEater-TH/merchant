package com.example.ui.admin

/**
 * Formats a shop setting number for an editable text field.
 * Whole numbers are shown without a trailing ".0" so the value round-trips
 * when the admin saves without changing it.
 */
fun formatSettingNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
