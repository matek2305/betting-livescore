package com.github.matek2305.betting.livescore

import com.natpryce.konfig.Configuration
import com.natpryce.konfig.Key
import com.natpryce.konfig.stringType

class BettingDatabaseConnectionProperties(private val config: Configuration) {

    fun getUrl(): String {
        return config[Key("betting-db.url", stringType)]
    }

    fun getUsername(): String {
        return config[Key("betting-db.username", stringType)]
    }

    fun getPassword(): String {
        return config[Key("betting-db.password", stringType)]
    }
}