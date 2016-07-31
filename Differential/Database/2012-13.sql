-- arsenal
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (1, 13, 1);
 
-- villa
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (2, 13, 2);
 
-- chelsea
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (7, 13, 3);
 
-- everton
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (8, 13, 4);
 
-- fulham
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (9, 13, 5);
 
-- liverpool
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (10, 13, 6);
 
-- man city
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (11, 13, 7);
 
-- man utd
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (12, 13, 8);
 
-- newcastle
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (13, 13, 9);
 
-- stoke
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (14, 13, 14);
 
-- sunderland
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (15, 13, 15);
 
-- spurs
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (16, 13, 17);
 
-- west brom
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (17, 13, 18);
 
-- wigan
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (19, 13, 20);
 
-- norwich
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (21, 13, 10);
 
-- qpr
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (22, 13, 11);
 
-- swansea
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (23, 13, 16);
 
-- west ham
INSERT INTO team_season (team_id, fpl_id, season, c_h_rating, c_h_gpg, c_h_gcpg, c_a_rating, c_a_gpg, c_a_gcpg)
 VALUES (18, 18, 12, 600, 1.34, 1.87, 375, 1.31, 1.58);
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (18, 13, 19);

UPDATE team SET name = 'West Ham', short_name = 'WHM', bbc_name = 'West Ham' WHERE _id = 18;

 
INSERT INTO team (_id, name, short_name, bbc_name, prem_name, active) VALUES (24, 'Southampton', 'SOU', 'Southampton', '', 1);
INSERT INTO team_season (team_id, fpl_id, season, c_h_rating, c_h_gpg, c_h_gcpg, c_a_rating, c_a_gpg, c_a_gcpg)
 VALUES (24, 24, 12, 690, 1.6, 1.29, 300, 1.18, 2.02);
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (24, 13, 13);

INSERT INTO team (_id, name, short_name, bbc_name, prem_name, active) VALUES (25, 'Reading', 'RDG', 'Reading', '', 1);
INSERT INTO team_season (team_id, fpl_id, season, c_h_rating, c_h_gpg, c_h_gcpg, c_a_rating, c_a_gpg, c_a_gcpg)
 VALUES (25, 25, 12, 625, 1.18, 1.29, 350, 1.08, 1.66);
INSERT INTO team_season (team_id, season, fpl_id)
 VALUES (25, 13, 12);

UPDATE gameweek SET season = 12;