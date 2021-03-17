CREATE TABLE MATCH_ENTITY
(
    ID BIGINT NOT NULL
        CONSTRAINT MATCH_ENTITY_PKEY
            PRIMARY KEY,
    AWAY_TEAM_NAME VARCHAR(255),
    AWAY_TEAM_SCORE INTEGER NOT NULL,
    CREATED_AT TIMESTAMP,
    FINISHED BOOLEAN NOT NULL,
    HOME_TEAM_NAME VARCHAR(255),
    HOME_TEAM_SCORE INTEGER NOT NULL,
    START_DATE_TIME TIMESTAMP,
    UPDATED_AT TIMESTAMP,
    UUID UUID,
    VERSION BIGINT NOT NULL
);

CREATE TABLE EXTERNAL_MATCH_ENTITY
(
    ID BIGINT NOT NULL
        CONSTRAINT EXTERNAL_MATCH_ENTITY_PKEY
            PRIMARY KEY,
    EXTERNAL_ID VARCHAR(255),
    ORIGIN VARCHAR(255),
    MATCH_ENTITY_ID BIGINT
);

INSERT INTO MATCH_ENTITY (ID, UUID, HOME_TEAM_NAME, AWAY_TEAM_NAME, START_DATE_TIME, FINISHED, HOME_TEAM_SCORE, AWAY_TEAM_SCORE, CREATED_AT, UPDATED_AT, VERSION) VALUES
(1, '14713275-41a6-4dc1-b545-ecc7f2783fb0', 'Manchester United', 'AC Milan', '2021-03-11 20:00:00.000000', true, 1, 1, current_timestamp, current_timestamp , 2),
(2, '14713275-41a6-4dc1-b545-ecc7f2783fb1', 'Real Madrid', 'Atalanta', '2021-03-16 20:00:00.000000', false, 0, 0, current_timestamp, current_timestamp , 1),
(3, '14713275-41a6-4dc1-b545-ecc7f2783fb2', 'Bayern Munich', 'Lazio', '2021-03-17 20:00:00.000000', false, 0, 0, current_timestamp, current_timestamp , 1),
(4, '14713275-41a6-4dc1-b545-ecc7f2783fb3', 'Chelsea', 'Atletico Madrid', '2021-03-17 20:00:00.000000', false, 0, 0, current_timestamp, current_timestamp , 1),
(5, '14713275-41a6-4dc1-b545-ecc7f2783fb4', 'AC Milan', 'Manchester United', '2021-03-18 20:00:00.000000', false, 0, 0, current_timestamp, current_timestamp , 1);

INSERT INTO EXTERNAL_MATCH_ENTITY (ID, EXTERNAL_ID, ORIGIN, MATCH_ENTITY_ID) VALUES
(1, '101', 'api-football', 1),
(2, '102', 'api-football', 2),
(3, '103', 'api-football', 3),
(4, '104', 'api-football', 4),
(5, '105', 'api-football', 5);



