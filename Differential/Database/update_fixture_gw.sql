UPDATE fixture
SET gameweek = (SELECT gameweek.num FROM gameweek WHERE gameweek.start_datetime <= (fixture.datetime+86000) AND gameweek.end_datetime >= (fixture.datetime+86000))