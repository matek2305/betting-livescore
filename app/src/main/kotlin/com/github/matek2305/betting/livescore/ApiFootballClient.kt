package com.github.matek2305.betting.livescore

import com.natpryce.konfig.Configuration
import com.natpryce.konfig.Key
import com.natpryce.konfig.stringType
import khttp.get
import java.time.LocalDate

class ApiFootballClient(private val config: Configuration) {

    fun fetchFinishedMatchesResultsByDate(date: LocalDate): List<MatchResult> {
        val response = get(
            url = "${config[url]}/fixtures",
            params = mapOf("status" to "FT", "date" to date.toString()),
            headers = mapOf("x-rapidapi-key" to config[rapidApiKey])
        ).jsonObject.getJSONArray("response")

        return (0 until response.length()).map {
            val entry = response.getJSONObject(it)
            val externalId = entry.getJSONObject("fixture").getInt("id").toString()
            val goals = entry.getJSONObject("goals")
            return@map MatchResult(externalId, goals.getInt("home"), goals.getInt("away"))
        }
    }

    companion object {
        private val url = Key("api-football.url", stringType)
        private val rapidApiKey = Key("api-football.rapid-api-key", stringType)
    }
}