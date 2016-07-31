-- arsenal
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (1, 16, 1);
 
-- villa
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (2, 16, 2);
 
-- chelsea
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (7, 16, 4);
 
-- palace
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (27, 16, 5);
 
-- everton
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (8, 16, 6);
 
-- leicester
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (30, 16, 7);
 
-- liverpool
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (10, 16, 8);
 
-- man city
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (11, 16, 9);
 
-- man utd
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (12, 16, 10);
 
-- newcastle
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (13, 16, 11);
 
-- stoke
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (14, 16, 15);
 
-- sunderland
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (15, 16, 16);
 
-- spurs
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (16, 16, 14);

-- west brom
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (17, 16, 19);

-- west ham
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (18, 16, 20);
 
-- swansea
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (23, 16, 17);
 
-- southampton
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (24, 16, 13);
 
--UPDATE team SET name = 'Spurs' WHERE _id = 16;

-- qpr, hull, burnley
UPDATE team SET active = 0 WHERE _id IN (22, 28, 29);
-- norwich
UPDATE team SET active = 1 WHERE _id IN (21);

-- bournemouth
INSERT INTO team (_id, name, short_name, bbc_name, prem_name, active) VALUES (31, 'Bournemouth', 'BOU', 'Bournemouth', 'Bournemouth', 1);
INSERT INTO team_season (team_id, fpl_id, season, c_h_rating, c_h_gpg, c_h_gcpg, c_a_rating, c_a_gpg, c_a_gcpg)
 VALUES (31, 31, 15, 640, 1.4, 1.7, 275, 0.8, 2.2);
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (31, 16, 3);

-- watford
INSERT INTO team (_id, name, short_name, bbc_name, prem_name, active) VALUES (32, 'Watford', 'WAT', 'Watford', 'Watford', 1);
INSERT INTO team_season (team_id, fpl_id, season, c_h_rating, c_h_gpg, c_h_gcpg, c_a_rating, c_a_gpg, c_a_gcpg)
 VALUES (32, 32, 15, 640, 1.4, 1.7, 275, 0.8, 2.2);
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (32, 16, 18);
 
-- norwich
INSERT INTO team_season (team_id, fpl_id, season, c_h_rating, c_h_gpg, c_h_gcpg, c_a_rating, c_a_gpg, c_a_gcpg)
 VALUES (21, 21, 15, 640, 1.4, 1.7, 275, 0.8, 2.2);
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (21, 16, 12);

UPDATE gameweek SET season = 16;
-- also update deadlines

--java:
-- seasons/seasons_desc in Player.java
-- run initial start of season to set fpl fixture IDs section in ScrpaeMatchScores_New + remove gw0 constraint in service (this is commented code in ScrapeMatchScores_New)
-- Selection.NUM_TEAMS