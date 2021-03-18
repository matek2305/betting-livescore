package com.github.matek2305.betting.livescore

import com.natpryce.konfig.*
import com.natpryce.konfig.Key
import java.time.*
import java.util.*
import khttp.get
import khttp.post
import khttp.structures.authorization.BasicAuthorization
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.`java-time`.datetime
import org.jetbrains.exposed.sql.transactions.transaction

object Matches : Table("match_entity") {
    val id = integer("id")
    val uuid = uuid("uuid")
    val startDateTime = datetime("start_date_time")
    val finished = bool("finished")

    override val primaryKey = PrimaryKey(id)
}

object ExternalMatches : Table("external_match_entity") {
    val id = integer("id")
    val externalId = varchar("external_id", length = 255)
    val origin = varchar("origin", length = 255)
    val matchEntityId = integer("match_entity_id").references(Matches.id)

    override val primaryKey = PrimaryKey(Matches.id)
}

data class FinishedMatch(val uuid: UUID, val date: LocalDate)

data class MatchResult(val homeTeamScore: Int, val awayTeamScore: Int)

val apiFootballUrl = Key("api-football.url", stringType)
val apiFootballRapidApiKey = Key("api-football.rapid-api-key", stringType)

val bettingRestApiUrl = Key("betting-rest-api.url", stringType)
val bettingRestApiUsername = Key("betting-rest-api.username", stringType)
val bettingRestApiPassword = Key("betting-rest-api.password", stringType)

val config = EnvironmentVariables() overriding ConfigurationProperties.fromResource("application.properties")

fun main(args: Array<String>) {
    val databaseConnectionProperties = BettingDatabaseConnectionProperties(config)
    val bettingDatabase = BettingDatabase(databaseConnectionProperties)

    val finishMatches = FinishMatches(TimeProvider.Default(), bettingDatabase)
    finishMatches.finish()
}
