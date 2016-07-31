select t1.name, t2.name
,f.pred_goals_home, f.pred_goals_away, f.pred_points_home, f.pred_points_away
,f.pred_ratio_home, f.pred_ratio_away
from team t1, team t2, fixture f 
where f.season=13 and f.gameweek=30
and t1._id = f.team_home_id and t2._id = f.team_away_id