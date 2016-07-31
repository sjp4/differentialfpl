-- arsenal
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (1, 15, 1);
 
-- villa
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (2, 15, 2);
 
-- chelsea
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (7, 15, 4);
 
-- palace
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (27, 15, 5);
 
-- everton
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (8, 15, 6);
 
-- hull
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (28, 15, 7);
 
-- liverpool
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (10, 15, 9);
 
-- man city
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (11, 15, 10);
 
-- man utd
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (12, 15, 11);
 
-- newcastle
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (13, 15, 12);
 
-- stoke
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (14, 15, 16);
 
-- sunderland
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (15, 15, 17);
 
-- spurs
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (16, 15, 15);

-- west brom
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (17, 15, 19);

-- west ham
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (18, 15, 20);
 
-- swansea
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (23, 15, 18);
 
-- southampton
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (24, 15, 14);
 
UPDATE team SET name = 'Spurs' WHERE _id = 16;

-- cardiff, fulham, norwich
UPDATE team SET active = 0 WHERE _id IN (26, 9, 21);
-- qpr
UPDATE team SET active = 1 WHERE _id IN (22);

-- burnley
INSERT INTO team (_id, name, short_name, bbc_name, prem_name, active) VALUES (29, 'Burnley', 'BUR', 'Burnley', 'Burnley', 1);
INSERT INTO team_season (team_id, fpl_id, season, c_h_rating, c_h_gpg, c_h_gcpg, c_a_rating, c_a_gpg, c_a_gcpg)
 VALUES (29, 29, 14, 690, 1.21, 1.08, 310, 1.14, 2.16);
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (29, 15, 3);

-- leicester
INSERT INTO team (_id, name, short_name, bbc_name, prem_name, active) VALUES (30, 'Leicester', 'LEI', 'Leicester', 'Leicester', 1);
INSERT INTO team_season (team_id, fpl_id, season, c_h_rating, c_h_gpg, c_h_gcpg, c_a_rating, c_a_gpg, c_a_gcpg)
 VALUES (30, 30, 14, 625, 1.70, 2.23, 250, 0.68, 2.23);
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (30, 15, 8);
 
 -- qpr
INSERT INTO team_season (team_id, fpl_id, season, c_h_rating, c_h_gpg, c_h_gcpg, c_a_rating, c_a_gpg, c_a_gcpg)
 VALUES (22, 22, 14, 625, 1.70, 2.23, 250, 0.68, 2.23);
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (22, 15, 13);

UPDATE gameweek SET season = 15;
-- also update deadlines

--java:
-- seasons/seasons_desc in Player.java
-- run initial start of season to set fpl fixture IDs section in ScrpaeMatchScores_New + remove gw0 constraint in service (this is commented code in ScrapeMatchScores_New)
-- Selection.NUM_TEAMS