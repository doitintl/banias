package com.doitintl.banias;

import com.google.api.services.bigquery.model.TableRow;
import org.apache.beam.sdk.values.TupleTag;
import org.json.JSONObject;

import java.util.*;

class MapEvents extends BaseMap {
    MapEvents(TupleTag<TableRow> errorsTag) {
        super(errorsTag);
    }

    @Override
	String getType() {
		return "events";
	}

    @Override
    void map(JSONObject json, TableRow tableRow) {
    	Iterator<String> keys = json.keys();

    	keys.forEachRemaining(key -> {
    		if(json.get(key) instanceof JSONObject){
				TableRow tmpTableRow = new TableRow();
    			map(json.getJSONObject(key), tmpTableRow);
    			tableRow.set(key,tmpTableRow);
			}
			else{
				tableRow.set(key, json.get(key));
			}
		});
    }
}
