package com.imsproject.watch.view.contracts

data class Result(val code: Code, val errorMessage: String?, val expId: String?) {
    enum class Code {
        OK,
        OK_EXPERIMENT_ENDED,
        CONNECTION_LOST,
        TCP_EXCEPTION,
        TCP_ERROR,
        UDP_EXCEPTION,
        UNEXPECTED_REQUEST,
        BAD_REQUEST,
        UNKNOWN_ERROR,
        GAME_ENDED_WITH_ERROR,
        SERVER_CLOSED_CONNECTION,
        BAD_RESOURCE;

        fun prettyName(): String{
            return this.name.replace("_", " ").lowercase()
        }
    }
}
