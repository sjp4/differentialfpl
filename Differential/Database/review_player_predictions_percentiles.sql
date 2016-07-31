select v.*
, (select round((sum(total) * 100.0) / (select count(*) from review_player_predictions_v), 1) from review_player_preds_grouped_v b 
   where b.total >= v.total) perc_cum
from review_player_preds_grouped_v v
order by total desc