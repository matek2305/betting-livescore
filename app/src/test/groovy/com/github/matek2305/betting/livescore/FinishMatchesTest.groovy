package com.github.matek2305.betting.livescore

import com.github.tomakehurst.wiremock.client.BasicCredentials
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import org.h2.tools.RunScript
import org.junit.Rule
import spock.lang.Specification
import spock.lang.Subject

import java.sql.DriverManager
import java.time.ZonedDateTime

import static com.github.matek2305.betting.livescore.LivescoreAppKt.config
import static com.github.tomakehurst.wiremock.client.WireMock.*

class FinishMatchesTest extends Specification {
    
    @Rule
    WireMockRule wireMockRule = new WireMockRule(8889)
    
    def bettingDatabase = new BettingDatabase(config)
    def apiFootballClient = new ApiFootballClient(config)
    def bettingApiClient = new BettingApiClient(config)
    def timeProviderMock = Mock(TimeProvider)
    
    @Subject
    def finishMatches = new FinishMatches(timeProviderMock, bettingDatabase, apiFootballClient, bettingApiClient)
    
    def setupSpec() {
        RunScript.execute(
                DriverManager.getConnection(
                        config.get(BettingDatabase.url) as String,
                        config.get(BettingDatabase.username) as String,
                        config.get(BettingDatabase.password) as String
                ),
                new FileReader('src/test/resources/data.sql')
        )
    }

    def setup() {
        wireMockRule.stubFor(
                post(urlPathEqualTo('/betting-rest-api/finished_matches'))
                        .willReturn(created())
        )
    }
    
    def "should use betting-rest-api to finish match with result from api-football"() {
        given:
            timeProviderMock.getCurrentDateTime() >> ZonedDateTime.parse('2021-03-16T22:56+01:00')
            
        and:
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
        
        when:
            finishMatches.finish()
        
        then:
            wireMockRule.verify(0, getRequestedFor(urlPathMatching('/api-football')))
        
        and:
            wireMockRule.verify(finishMatchRequest('14713275-41a6-4dc1-b545-ecc7f2783fb1', 3, 1))
    }
    
    def "should fetch results from api-football using dates range"() {
        given:
            timeProviderMock.getCurrentDateTime() >> ZonedDateTime.parse('2021-03-20T06:37+01:00')
    
        and:
            wireMockRule.stubFor(
                    get(urlPathEqualTo('/api-football/fixtures'))
                        
                            .withQueryParam('status', equalTo('FT'))
                            .withQueryParam('from', equalTo('2021-03-16'))
                            .withQueryParam('to', equalTo('2021-03-18'))
                            .withHeader('x-rapidapi-key', equalTo('rapid-api-key'))
                        
                            .willReturn(afResponse(
                                    afResponseEntry(202, 1, 1),
                                    afResponseEntry(102, 3, 1),
                                    afResponseEntry(103, 2, 1),
                                    afResponseEntry(104, 2, 0),
                                    afResponseEntry(105, 0, 1)
                            ))
            )
    
        when:
            finishMatches.finish()
        
        then:
            wireMockRule.verify(0, getRequestedFor(urlPathMatching('/api-football')))
    
        and:
            wireMockRule.verify(finishMatchRequest('14713275-41a6-4dc1-b545-ecc7f2783fb1', 3, 1))
            wireMockRule.verify(finishMatchRequest('14713275-41a6-4dc1-b545-ecc7f2783fb2', 2, 1))
            wireMockRule.verify(finishMatchRequest('14713275-41a6-4dc1-b545-ecc7f2783fb3', 2, 0))
            wireMockRule.verify(finishMatchRequest('14713275-41a6-4dc1-b545-ecc7f2783fb4', 0, 1))
    }
    
    def "should not call football-api when there are no matches applicable for finish found"() {
        given:
            timeProviderMock.getCurrentDateTime() >> ZonedDateTime.parse('2021-03-16T22:54+01:00')
        
        when:
            finishMatches.finish()
        
        then:
            wireMockRule.verify(0, getRequestedFor(urlPathMatching('/api-football')))
    }
    
    def "should not call betting-rest-api when there is not result found for match which supposed to be finished"() {
        given:
            timeProviderMock.getCurrentDateTime() >> ZonedDateTime.parse('2021-03-16T21:56Z')
    
        and:
            wireMockRule.stubFor(
                    get(urlPathEqualTo('/api-football/fixtures'))
                        
                            .withQueryParam('status', equalTo('FT'))
                            .withQueryParam('date', equalTo('2021-03-16'))
                            .withHeader('x-rapidapi-key', equalTo('rapid-api-key'))
                        
                            .willReturn(afResponse(
                                    afResponseEntry(202, 1, 1),
                            ))
            )
    
        when:
            finishMatches.finish()
    
        then:
            wireMockRule.verify(0, postRequestedFor(urlPathEqualTo('/betting-rest-api/finished_matches')))
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
    
    private static RequestPatternBuilder finishMatchRequest(String uuid, int homeTeamScore, int awayTeamScore) {
        return postRequestedFor(urlPathEqualTo('/betting-rest-api/finished_matches'))
                .withBasicAuth(new BasicCredentials('betting', 'betting'))
                .withRequestBody(equalToJson("""
                            {
                              "matchId": "$uuid",
                              "homeTeamScore": $homeTeamScore,
                              "awayTeamScore": $awayTeamScore
                            }
                            """))
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
