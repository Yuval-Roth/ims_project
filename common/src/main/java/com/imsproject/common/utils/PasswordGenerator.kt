package com.imsproject.common.utils

import java.util.*

object PasswordGenerator {

    fun generatePassword(length : Int, specialChars : Boolean = true): String {
        val rand = Random()
        val chars = CharArray(length) { ' ' }
        val specials = "!@#$%^&*()\\-=[]{};':\"<>?|"
        var i = 0
        fun next(num : Int) : Char {
            return when(num % 4) {
                0 -> {
                    if (specialChars){
                        specials[rand.nextInt(specials.length)]
                    } else {
                        rand.nextInt('a'.code, 'z'.code).toChar()
                    }
                }
                1 -> rand.nextInt('a'.code, 'z'.code).toChar()
                2 -> rand.nextInt('A'.code, 'Z'.code).toChar()
                3 -> rand.nextInt('0'.code, '9'.code).toChar()
                else -> throw Exception("WTF?")
            }
        }
        while (i < length) {
            chars[i] = next(i)
            i++
        }
        chars.shuffle()
        return String(chars)
    }
}