package com.imsproject.watch.view.contracts

data class Result(val code: Code, val errorMessage: String? = null) {
    enum class Code {
        OK,
        CONNECTION_LOST,
        TCP_EXCEPTION,
        TCP_ERROR,
        UDP_EXCEPTION,
        UNEXPECTED_REQUEST,
        BAD_REQUEST,
        UNKNOWN_ERROR;

        fun prettyName(): String{
            return this.name.replace("_", " ").lowercase()
        }
    }
}
