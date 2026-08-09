package de.crazybatto.solelink.util

fun ByteArray.toHexString(separator: String = " "): String =
    joinToString(separator) { byte -> "%02X".format(byte.toInt() and 0xFF) }

fun ByteArray.toPrintableAscii(): String =
    buildString(size) {
        this@toPrintableAscii.forEach { byte ->
            val value = byte.toInt() and 0xFF
            append(if (value in 32..126) value.toChar() else '.')
        }
    }
