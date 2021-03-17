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
    val matchStartDateTimeThatShouldHaveFinishedByNow = LocalDateTime.now().minusMinutes(115).atZone(ZoneId.systemDefault())
        .withZoneSameInstant(ZoneOffset.UTC)
        .toLocalDateTime()

    val databaseConnectionProperties = BettingDatabaseConnectionProperties(config)
    val bettingDatabase = BettingDatabase(databaseConnectionProperties)

    println("Searching for matches that should have finished by now, started before $matchStartDateTimeThatShouldHaveFinishedByNow ...")
    val matchesToSearchForResult = bettingDatabase.getAllNotFinishedMatchesByStartDateTimeFrom(matchStartDateTimeThatShouldHaveFinishedByNow)
    if (matchesToSearchForResult.isEmpty()) {
        println("No matches found")
        return
    }

    println("Found matches: ${matchesToSearchForResult.size}")

    val matchDates = matchesToSearchForResult.values.map { it.date }.toSet()
    val resultForExternalId = fetchFinishedMatchesResultsByDates(matchDates)

    println("Found ${resultForExternalId.size} finished matches")

    matchesToSearchForResult.forEach { (externalId, match) ->

        println("Search for match result (externalId=$externalId, uuid=${match.uuid}) ...")
        val matchResult = resultForExternalId[externalId]
        if (matchResult == null) {
            println("Result not found for match (externalId=$externalId, uuid=${match.uuid})")
            return@forEach
        }

        println("Result found ($matchResult) for match (externalId=$externalId, uuid=${match.uuid}), preparing finish request ...")
        finishMatch(match.uuid, matchResult)
    }
}

private fun fetchFinishedMatchesResultsByDates(dates: Set<LocalDate>): Map<String, MatchResult> {
    if (dates.size != 1) {
        throw IllegalArgumentException("Date range not supported yet")
    }

    val response = get(
        url = "${config[apiFootballUrl]}/fixtures",
        params = mapOf("status" to "FT", "date" to dates.first().toString()),
        headers = mapOf("x-rapidapi-key" to config[apiFootballRapidApiKey])
    ).jsonObject.getJSONArray("response")

    return (0 until response.length()).map {
        val entry = response.getJSONObject(it)
        val externalId = entry.getJSONObject("fixture").getInt("id").toString()
        val goals = entry.getJSONObject("goals")
        return@map externalId to MatchResult(goals.getInt("home"), goals.getInt("away"))
    }.toMap()
}

private fun finishMatch(matchUUID: UUID, matchResult: MatchResult) {
    val finishMatchResponseStatusCode = post(
        url = "${config[bettingRestApiUrl]}/finished_matches",
        auth = BasicAuthorization(config[bettingRestApiUsername], config[bettingRestApiPassword]),
        json = mapOf(
            "matchId" to matchUUID.toString(),
            "homeTeamScore" to matchResult.homeTeamScore,
            "awayTeamScore" to matchResult.awayTeamScore
        )
    ).statusCode

    if (finishMatchResponseStatusCode == 201) {
        println("Match (uuid=$matchUUID) successfully finished with result: $matchResult")
    } else {
        println("Match (uuid=$matchUUID) finish failed with status: $finishMatchResponseStatusCode")
    }
}
