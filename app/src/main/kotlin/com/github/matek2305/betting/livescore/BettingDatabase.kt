package com.github.matek2305.betting.livescore

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class BettingDatabase(private val properties: BettingDatabaseConnectionProperties) {

    fun getAllNotFinishedMatchesByStartDateTimeFrom(from: LocalDateTime): Map<String, FinishedMatch> {
        println("Connecting to betting_db at ${properties.getUrl()} ...")

        Database.connect(
            url = properties.getUrl(),
            user = properties.getUsername(),
            password = properties.getPassword()
        )

        println("Connected!")

        return transaction {
            addLogger(StdOutSqlLogger)

            return@transaction ExternalMatches.innerJoin(Matches)
                .select {
                    Matches.startDateTime.less(from)
                        .and(Matches.finished.eq(false))
                        .and(ExternalMatches.origin.eq("api-football"))
                }
                .map { it[ExternalMatches.externalId] to FinishedMatch(it[Matches.uuid], it[Matches.startDateTime].toLocalDate()) }
                .toMap()
        }
    }
}