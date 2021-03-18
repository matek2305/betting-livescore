package com.github.matek2305.betting.livescore

import khttp.get
import khttp.post
import khttp.structures.authorization.BasicAuthorization
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*

class FinishMatches(private val timeProvider: TimeProvider, private val bettingDatabase: BettingDatabase) {

    fun finish() {
        val matchStartDateTimeThatShouldHaveFinishedByNow =
            timeProvider.getCurrentDateTime().minusMinutes(115)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime()

        println("Searching for matches that should have finished by now, started before $matchStartDateTimeThatShouldHaveFinishedByNow ...")
        val matchesToSearchForResult =
            bettingDatabase.getAllNotFinishedMatchesByStartDateTimeFrom(matchStartDateTimeThatShouldHaveFinishedByNow)
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
}