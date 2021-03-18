package com.github.matek2305.betting.livescore

import com.natpryce.konfig.Configuration
import com.natpryce.konfig.Key
import com.natpryce.konfig.stringType
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class BettingDatabase(private val config: Configuration) {

    fun getAllNotFinishedMatchesByStartDateTimeFrom(from: LocalDateTime): List<NotFinishedMatch> {
        println("Connecting to betting_db at ${config[url]} ...")
        Database.connect(url = config[url], user = config[username], password = config[password])
        println("Connected!")

        return transaction {
            addLogger(StdOutSqlLogger)

            return@transaction ExternalMatch.innerJoin(Match)
                .select {
                    Match.startDateTime.less(from)
                        .and(Match.finished.eq(false))
                        .and(ExternalMatch.origin.eq("api-football"))
                }
                .map {
                    NotFinishedMatch(
                        it[Match.uuid],
                        it[ExternalMatch.externalId],
                        it[Match.startDateTime].toLocalDate())
                }
        }
    }

    companion object {
        val url = Key("betting-db.url", stringType)
        val username = Key("betting-db.username", stringType)
        val password = Key("betting-db.password", stringType)
    }
}