package com.imsproject.common.utils

import java.util.*

object PasswordGenerator {

    fun generatePassword(): String {
        val rand = Random()
        val chars = CharArray(32) { ' ' }
        val specialChars = "!@#$%^&*()\\-=\\[\\]{};':\"<>?|"
        var i = 0
        while (i < 32) {
            chars[i++] = (specialChars[rand.nextInt(specialChars.length)])
            chars[i++] = (rand.nextInt('a'.code, 'z'.code).toChar())
            chars[i++] = (rand.nextInt('A'.code, 'Z'.code).toChar())
            chars[i++] = (rand.nextInt('0'.code, '9'.code).toChar())
        }
        chars.shuffle()
        return String(chars)
    }
}