package com.github.matek2305.betting.livescore

import java.lang.IllegalArgumentException
import java.time.ZoneOffset
import java.util.*

class FinishMatches(
    private val timeProvider: TimeProvider,
    private val bettingDatabase: BettingDatabase,
    private val apiFootballClient: ApiFootballClient,
    private val bettingApiClient: BettingApiClient) {

    fun finish() {
        println("Searching for matches supposed to be finished by now ...")
        val matches = getMatchesWhichSupposedToBeFinished()
        if (matches.isEmpty()) {
            println("No matches applicable for finish found")
            return
        }

        println("Found matches: ${matches.size}, fetching results ...")
        fetchResultsFor(matches).forEach { (matchUUID, result) ->
            println("Match(uuid=$matchUUID) result found: ${result.homeTeamScore} - ${result.awayTeamScore}")
            val responseStatus = bettingApiClient.finishMatch(matchUUID, result).statusCode
            if (responseStatus == 201) {
                println("Match(uuid=$matchUUID) successfully finished with found result")
            } else {
                println("Match(uuid=$matchUUID) finish failed with status: $responseStatus")
            }
        }
    }

    private fun getMatchesWhichSupposedToBeFinished(): List<NotFinishedMatch> {
        val matchStartDateTimeThatShouldHaveFinishedByNow =
            timeProvider.getCurrentDateTime().minusMinutes(TIME_IN_MINUTES_AFTER_WHICH_MATCH_SHOULD_FINISH)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime()

        return bettingDatabase.getAllNotFinishedMatchesByStartDateTimeFrom(matchStartDateTimeThatShouldHaveFinishedByNow)
    }

    private fun fetchResultsFor(matches: List<NotFinishedMatch>): Map<UUID, MatchResult> {
        val dates = matches.map { it.date }.toSet()

        val results = if (dates.size > 1) {
            apiFootballClient.fetchFinishedMatchesResultsByDates(dates.min()!!, dates.max()!!)
        } else {
            apiFootballClient.fetchFinishedMatchesResultsByDate(dates.first())
        }

        val resultsForExternalId = results.map { it.externalId to it }.toMap()

        return matches
            .filter { resultsForExternalId.containsKey(it.externalId) }
            .map { it.uuid to resultsForExternalId.getValue(it.externalId) }
            .toMap()

    }

    companion object {
        private const val TIME_IN_MINUTES_AFTER_WHICH_MATCH_SHOULD_FINISH = 115L
    }
}