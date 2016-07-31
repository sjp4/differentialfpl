select p.name, (ps.pred_season_pts / 100) points, (ps.price / 10.0) price
, t.name team, ps.position
from player p, player_season ps, team t
where p._id=ps.player_id
and ps.season=12 
--and ps.position=2
and p.team_id = t._id
order by 2 desc
