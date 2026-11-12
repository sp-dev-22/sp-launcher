package com.example.util

import java.security.MessageDigest

/**
 * App Lock (v1.14.0): one PIN for the whole launcher, following the SP WEB OS v6.1
 * design — a 4-6 digit PIN, stored only as its SHA-256 hash. The plaintext PIN is
 * never stored anywhere, and never leaves the PIN dialog that read it.
 */
object AppLock {

    /** A PIN is 4-6 digits, nothing else. */
    fun isValidPin(pin: String): Boolean =
        pin.length in 4..6 && pin.all { it.isDigit() }

    /** SHA-256 hex of the PIN. */
    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /** True when the typed PIN hashes to the stored hash. An empty stored hash never matches. */
    fun pinMatches(pin: String, hash: String): Boolean =
        hash.isNotEmpty() && hashPin(pin) == hash
}
