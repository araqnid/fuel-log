package org.araqnid.fuellog.test

import com.google.common.io.ByteProcessor
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class DigestProcessor(algorithm: String) : ByteProcessor<String> {
    companion object {
        fun hexDigest(bytes: ByteArray, algorithm: String): String {
            val processor = DigestProcessor(algorithm)
            processor.processBytes(bytes, 0, bytes.size)
            return processor.result
        }
    }

    private val digest: MessageDigest = try {
        MessageDigest.getInstance(algorithm)
    } catch (e: NoSuchAlgorithmException) {
        throw IllegalArgumentException("Invalid digest algorithm: $algorithm", e)
    }

    override fun processBytes(buf: ByteArray, off: Int, len: Int): Boolean {
        digest.update(buf, off, len)
        return true
    }

    override fun getResult(): String {
        val hexDigits = "0123456789abcdef"
        val digestBytes = digest.digest()
        val digestChars = CharArray(digestBytes.size * 2)
        for (i in digestBytes.indices) {
            val b = digestBytes[i].toInt()
            digestChars[i * 2] = hexDigits[b and 0xf0 shr 4]
            digestChars[i * 2 + 1] = hexDigits[b and 0x0f]
        }
        return String(digestChars)
    }
}