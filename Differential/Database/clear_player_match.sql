delete from player_match where (season = 12 and gameweek <= 19) and (minutes is null or minutes < 1);
delete from player_match where season < 12 and (minutes is null or minutes < 1);
delete from player_match where player_player_id not in (select player_id from player_season where season = 12);