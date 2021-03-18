package com.github.matek2305.betting.livescore

import com.natpryce.konfig.Configuration
import com.natpryce.konfig.Key
import com.natpryce.konfig.stringType
import khttp.post
import khttp.responses.Response
import khttp.structures.authorization.BasicAuthorization
import java.util.*

class BettingApiClient(private val config: Configuration) {

    fun finishMatch(matchUUID: UUID, matchResult: MatchResult): Response = post(
        url = "${config[url]}/finished_matches",
        auth = BasicAuthorization(config[username], config[password]),
        json = mapOf(
            "matchId" to matchUUID.toString(),
            "homeTeamScore" to matchResult.homeTeamScore,
            "awayTeamScore" to matchResult.awayTeamScore
        )
    )

    companion object {
        private val url = Key("betting-rest-api.url", stringType)
        private val username = Key("betting-rest-api.username", stringType)
        private val password = Key("betting-rest-api.password", stringType)
    }
}