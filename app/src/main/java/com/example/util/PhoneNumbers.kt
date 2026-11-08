package com.example.util

/**
 * Phone-number plumbing for the SP Assistant (v1.12).
 *
 * Numbers must work however the user saved or typed them: with a country code, without
 * one, with spaces or dashes, or with a leading 0. Indian numbers are the home case —
 * a bare 10-digit number gains the 91 country code wherever an international form is
 * needed (WhatsApp's wa.me links refuse anything else).
 */
object PhoneNumbers {

    fun digits(raw: String): String = raw.filter { it.isDigit() }

    /**
     * International digits for wa.me: "098765 43210", "+91 98765-43210", "9876543210"
     * and "919876543210" all come out as "919876543210".
     */
    fun international(raw: String): String {
        var d = digits(raw)
        if (d.startsWith("00")) d = d.drop(2)
        return when {
            d.length == 10 -> "91$d"
            d.length == 11 && d.startsWith("0") -> "91" + d.drop(1)
            else -> d
        }
    }

    /** True when the search text itself is a phone number (7+ digits, phone characters only). */
    fun looksLikeNumber(query: String): Boolean {
        val t = query.trim()
        if (t.isEmpty()) return false
        if (!t.all { it.isDigit() || it == '+' || it == '-' || it == '(' || it == ')' || it == ' ' }) {
            return false
        }
        return digits(t).length >= 7
    }
}
