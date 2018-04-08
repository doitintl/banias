package com.doitintl.banias;

import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import org.apache.beam.sdk.values.TupleTag;
import org.json.JSONObject;

import java.util.*;

class MapEvents extends BaseMap {
	private HashMap<String, TableSchema> tableSchema;
	private String tableDestinationPrefix;

    MapEvents(TupleTag<TableRow> errorsTag, HashMap<String, TableSchema> tableSchema, String tableDestinationPrefix) {
        super(errorsTag);
		this.tableSchema = tableSchema;
		this.tableDestinationPrefix = tableDestinationPrefix;
    }

    @Override
	String getType() {
		return "events";
	}

    @Override
    void map(JSONObject json, TableRow tableRow) {
		String tableName = tableDestinationPrefix;
		JSONObject event = json.getJSONObject("Event");
		JSONObject type = event.getJSONObject("type");

		if (type.has("event_name")) {
			String str = type.get("event_name").toString();
			tableRow.set("event_name", str);
			tableName += str;
		}
		if (type.has("event_version")) {
			String str = type.get("event_version").toString();
			tableRow.set("event_version", str);
			tableName += str;
		}

		if (event.has("payload")) {
			JSONObject payload = event.getJSONObject("payload");
			Iterator<String> keys = payload.keys();
			keys.forEachRemaining(key -> tableRow.set(key, payload.get(key).toString()));
		}

		createSchema(json, tableName);
	}

	private void createSchema(JSONObject json, String tableName){
    	if (tableSchema.get(tableName) == null){
			Iterator<String> keys = json.keys();
			ArrayList<TableFieldSchema> fields = new ArrayList<>();

			keys.forEachRemaining(key->{
				if (key.equalsIgnoreCase("Event")){
					JSONObject payload = json.getJSONObject("Event").getJSONObject("payload");
					Iterator<String> payloadKeys = payload.keys();

					payloadKeys.forEachRemaining(payloadKey -> fields.add(new TableFieldSchema().setName(payloadKey).setType("STRING")));
				}else {
					TableFieldSchema tableFieldSchema = new TableFieldSchema().setName(key).setType("STRING");
					fields.add(tableFieldSchema);
				}

			});

			BaniasPipeline.updateTableSchema(tableName, new TableSchema().setFields(fields));
    	}
	}
}
