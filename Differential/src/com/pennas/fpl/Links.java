package com.pennas.fpl;

import java.util.ArrayList;

import com.pennas.fpl.util.FPLActivity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;


public class Links extends FPLActivity {
	
	private ListView listViewRef;
	ArrayList<LinkEntry> links = new ArrayList<LinkEntry>();
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        actionBar.setTitle("Links");
        
        setContentView(R.layout.links_screen);
        
        listViewRef = (ListView) findViewById(android.R.id.list);
        
	 	addLink("Fantasy Football Scout", "http://www.fantasyfootballscout.co.uk/"
	 			, "Expert fantasy football advice, weekly picks and insight");
	 	addLink("PhysioRoom Injury Table", "http://www.physioroom.com/news/english_premier_league/epl_injury_table.php"
	 			, "Detailed and regularly-updated injury lists for each Premier League team");
	 	addLink("BBC Sport - Premiership", "http://news.bbc.co.uk/sport1/hi/football/eng_prem/default.stm"
	 			, "Excellent matchday scores resource, plus news, tables, etc");
	 	addLink("Fantasy Football Scout Twitter", "http://twitter.com/#!/FFScout"
	 			, "Continually-updated injury/selection news");
	 	addLink("FA Suspensions List", "http://www.thefa.com/TheFA/Disciplinary/SuspensionLists/"
	 			, "Official info");
	 	addLink("Total FPL", "http://totalfpl.com/"
	 			, "Estimated price-change info, and more new/articles");
	 	addLink("Total FPL Twitter", "http://twitter.com/#!/totalfpl"
	 			, "Another regularly-updated feed of injury news etc");
	 	addLink("PLFantasy", "http://premierleaguefantasy.blogspot.com/"
	 			, "Deep statistical analysis of FPL points-scoring");
        
        LinkEntryAdapter plAdapter = new LinkEntryAdapter(this, R.layout.links_screen_item, links);
    	listViewRef.setAdapter(plAdapter);
    	
    	// listener for stat click: display players screen sorted by that stat
		OnItemClickListener linksListListener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
	            // load right stats screen
				LinkEntry it = links.get(position);
				//Uri uri = Uri.parse( "http://search.twitter.com/search?q=" + Uri.encode(playerName + " " + teamName));
				final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(it.url));
				startActivity(intent);
	        }
	    };
	    listViewRef.setOnItemClickListener(linksListListener);
    }
    
    private void addLink(String title, String url, String desc) {
    	LinkEntry l = new LinkEntry();
    	l.title = title;
    	l.url = url;
    	l.description = desc;
    	links.add(l);
    }
    
    private static class LinkEntry {
    	String url;
    	String title;
    	String description;
    }
    
    // adapater mapping player stats array to listview item
	private class LinkEntryAdapter extends ArrayAdapter<LinkEntry> {
		//private ArrayList<LinkEntry> items;
		
		public LinkEntryAdapter(Context context, int textViewResourceId, ArrayList<LinkEntry> items) {
			super(context, textViewResourceId, items);
			//this.items = items;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		        v = vi.inflate(R.layout.links_screen_item, null);
			}
			LinkEntry it = links.get(position);
			if (it != null) {
				TextView t = (TextView) v.findViewById(R.id.title);
				t.setText(it.title);
				t = (TextView) v.findViewById(R.id.description);
				t.setText(it.description);
			}
			return v;
		}
	}
    
}