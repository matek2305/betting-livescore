package com.github.matek2305.betting.livescore

import com.github.tomakehurst.wiremock.client.BasicCredentials
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.junit.WireMockRule
import org.h2.tools.RunScript
import org.junit.Rule
import spock.lang.Specification

import java.sql.DriverManager

import static com.github.tomakehurst.wiremock.client.WireMock.*

class LivescoreSpecification extends Specification {
    
    @Rule
    WireMockRule wireMockRule = new WireMockRule(8889)
    
    void setup() {
        def connectionProperties = new BettingDatabaseConnectionProperties(LivescoreAppKt.config)
        RunScript.execute(DriverManager.getConnection(connectionProperties.getUrl(), connectionProperties.getUsername(), connectionProperties.getPassword()), new FileReader('src/test/resources/data.sql'))
    
        wireMockRule.stubFor(
                post(urlPathEqualTo('/betting-rest-api/finished_matches'))
                        .willReturn(created())
        )
    }
    
    def "should finish match with result fetched from api-football"() {
        given:
            wireMockRule.stubFor(
                    get(urlPathEqualTo('/api-football/fixtures'))
                    
                            .withQueryParam('status', equalTo('FT'))
                            .withQueryParam('date', equalTo('2021-03-16'))
                            .withHeader('x-rapidapi-key', equalTo('rapid-api-key'))
                            
                            .willReturn(afResponse(
                                    afResponseEntry(202, 1, 1),
                                    afResponseEntry(102, 3, 1)
                            ))
            )
        
        expect:
            LivescoreAppKt.main()
        
        and:
            wireMockRule.verify(
                    postRequestedFor(urlPathEqualTo('/betting-rest-api/finished_matches'))
                            .withBasicAuth(new BasicCredentials('betting', 'betting'))
                            .withRequestBody(equalToJson("""
                            {
                              "matchId": "14713275-41a6-4dc1-b545-ecc7f2783fb1",
                              "homeTeamScore": 3,
                              "awayTeamScore": 1
                            }
                            """))
            )
    }
    
    private static ApiFootballResponseEntry afResponseEntry(Integer fixtureId, Integer homeTeamGoals, Integer awayTeamGoals) {
        return new ApiFootballResponseEntry(fixtureId, homeTeamGoals, awayTeamGoals)
    }
    
    private static ResponseDefinitionBuilder afResponse(ApiFootballResponseEntry... entries) {
        def responseArrayContent = entries.collect {
            """
            {
              "fixture": {
                "id": ${it.fixtureId}
              },
              "goals": {
                "home": ${it.homeTeamGoals},
                "away": ${it.awayTeamGoals}
              }
            }
            """
        }.join(',')
        
        return okJson(""" { "response": [${responseArrayContent}] }""")
    }
    
    static class ApiFootballResponseEntry {
        final Integer fixtureId
        final Integer homeTeamGoals
        final Integer awayTeamGoals
    
        ApiFootballResponseEntry(Integer fixtureId, Integer homeTeamGoals, Integer awayTeamGoals) {
            this.fixtureId = fixtureId
            this.homeTeamGoals = homeTeamGoals
            this.awayTeamGoals = awayTeamGoals
        }
    }
}
