UPDATE player_match
SET opp_team_id = (SELECT fixture.team_away_id FROM fixture WHERE fixture._id = player_match.fixture_id)
WHERE is_home = 1;
UPDATE player_match
SET opp_team_id = (SELECT fixture.team_home_id FROM fixture WHERE fixture._id = player_match.fixture_id)
WHERE is_away = 1;