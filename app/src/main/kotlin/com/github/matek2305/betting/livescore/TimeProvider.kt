package com.github.matek2305.betting.livescore

import java.time.ZonedDateTime

interface TimeProvider {

    fun getCurrentDateTime(): ZonedDateTime

    class Default : TimeProvider {
        override fun getCurrentDateTime(): ZonedDateTime {
            return ZonedDateTime.now()
        }
    }
}