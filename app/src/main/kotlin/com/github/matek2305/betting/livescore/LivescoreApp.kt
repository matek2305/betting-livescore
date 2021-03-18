package com.github.matek2305.betting.livescore

import com.natpryce.konfig.*
import com.natpryce.konfig.Key
import java.time.*
import java.util.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.`java-time`.datetime

object Match : Table("match_entity") {
    val id = integer("id")
    val uuid = uuid("uuid")
    val startDateTime = datetime("start_date_time")
    val finished = bool("finished")

    override val primaryKey = PrimaryKey(id)
}

object ExternalMatch : Table("external_match_entity") {
    val id = integer("id")
    val externalId = varchar("external_id", length = 255)
    val origin = varchar("origin", length = 255)
    val matchEntityId = integer("match_entity_id").references(Match.id)

    override val primaryKey = PrimaryKey(Match.id)
}

data class NotFinishedMatch(val uuid: UUID, val externalId: String, val date: LocalDate)

data class MatchResult(val externalId: String, val homeTeamScore: Int, val awayTeamScore: Int)

val config = EnvironmentVariables() overriding ConfigurationProperties.fromResource("application.properties")

fun main(args: Array<String>) {
    val bettingDatabase = BettingDatabase(config)
    val apiFootballClient = ApiFootballClient(config)
    val bettingApiClient = BettingApiClient(config)

    val finishMatches = FinishMatches(TimeProvider.Default(), bettingDatabase, apiFootballClient, bettingApiClient)
    finishMatches.finish()
}
