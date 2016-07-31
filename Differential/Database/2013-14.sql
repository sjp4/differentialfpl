-- arsenal
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (1, 14, 1);
 
-- villa
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (2, 14, 2);
 
-- chelsea
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (7, 14, 4);
 
-- everton
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (8, 14, 6);
 
-- fulham
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (9, 14, 7);
 
-- liverpool
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (10, 14, 9);
 
-- man city
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (11, 14, 10);
 
-- man utd
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (12, 14, 11);
 
-- newcastle
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (13, 14, 12);
 
-- stoke
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (14, 14, 15);
 
-- sunderland
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (15, 14, 16);
 
-- spurs
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (16, 14, 18);
 
-- west brom
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (17, 14, 19);

-- west ham
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (18, 14, 20);
 
-- norwich
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (21, 14, 13);
 
-- swansea
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (23, 14, 17);
 
-- southampton
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (24, 14, 14);
 
UPDATE team SET name = 'West Ham', short_name = 'WHU', bbc_name = 'West Ham' WHERE _id = 18;

-- reading, qpr, wigan
UPDATE team SET active = 0 WHERE _id IN (25, 22, 19);

-- cardiff
INSERT INTO team (_id, name, short_name, bbc_name, prem_name, active) VALUES (26, 'Cardiff City', 'CAR', 'Cardiff City', 'Cardiff City', 1);
INSERT INTO team_season (team_id, fpl_id, season, c_h_rating, c_h_gpg, c_h_gcpg, c_a_rating, c_a_gpg, c_a_gcpg)
 VALUES (26, 26, 13, 690, 1.21, 1.08, 310, 1.14, 2.16);
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (26, 14, 3);

-- palace
INSERT INTO team (_id, name, short_name, bbc_name, prem_name, active) VALUES (27, 'Crystal Palace', 'CRY', 'Crystal Palace', 'Crystal Palace', 1);
INSERT INTO team_season (team_id, fpl_id, season, c_h_rating, c_h_gpg, c_h_gcpg, c_a_rating, c_a_gpg, c_a_gcpg)
 VALUES (27, 26, 13, 625, 1.70, 2.23, 250, 0.68, 2.23);
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (27, 14, 5);
 
 -- hull
INSERT INTO team (_id, name, short_name, bbc_name, prem_name, active) VALUES (28, 'Hull City', 'HUL', 'Hull City', 'Hull City', 1);
INSERT INTO team_season (team_id, fpl_id, season, c_h_rating, c_h_gpg, c_h_gcpg, c_a_rating, c_a_gpg, c_a_gcpg)
 VALUES (28, 26, 13, 615, 1.14, 1.58, 300, 0.85, 2.16);
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (28, 14, 8);

UPDATE gameweek SET season = 14;
-- also update deadlines

--java:
-- seasons/seasons_desc in Player.java
-- run initial start of season to set fpl fixture IDs section in ScrpaeMatchScores_New + remove gw0 constraint in service
-- Selection.NUM_TEAMS