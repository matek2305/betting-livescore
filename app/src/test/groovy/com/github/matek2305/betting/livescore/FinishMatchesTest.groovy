package com.github.matek2305.betting.livescore

import com.github.tomakehurst.wiremock.client.BasicCredentials
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.junit.WireMockRule
import org.h2.tools.RunScript
import org.junit.Rule
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

import java.sql.DriverManager
import java.time.ZonedDateTime

import static com.github.matek2305.betting.livescore.LivescoreAppKt.config
import static com.github.tomakehurst.wiremock.client.WireMock.*

class FinishMatchesTest extends Specification {
    
    @Rule
    WireMockRule wireMockRule = new WireMockRule(8889)
    
    @Shared
    def connectionProperties = new BettingDatabaseConnectionProperties(config)
    def bettingDatabase = new BettingDatabase(connectionProperties)
    def timeProviderMock = Mock(TimeProvider)
    
    @Subject
    def finishMatches = new FinishMatches(timeProviderMock, bettingDatabase)
    
    def setupSpec() {
        RunScript.execute(
                DriverManager.getConnection(
                        connectionProperties.getUrl(),
                        connectionProperties.getUsername(),
                        connectionProperties.getPassword()
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
            timeProviderMock.getCurrentDateTime() >> ZonedDateTime.parse('2021-03-16T21:56Z')
            
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
        
        expect:
            finishMatches.finish()
        
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
        
        and:
            wireMockRule.verify(0, getRequestedFor(urlPathMatching('/api-football')))
    }
    
    def "should not call football-api when there are no matches applicable for finish found"() {
        given:
            timeProviderMock.getCurrentDateTime() >> ZonedDateTime.parse('2021-03-16T21:54Z')
        
        expect:
            finishMatches.finish()
        
        and:
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
    
        expect:
            finishMatches.finish()
    
        and:
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
