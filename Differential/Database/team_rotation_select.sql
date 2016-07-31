select t1.name, t2.name, tr.* from team_rotation tr, team t1, team t2
where t1._id = tr.team_a and t2._id = tr.team_b
order by gameweeks_home desc, cs_perc desc