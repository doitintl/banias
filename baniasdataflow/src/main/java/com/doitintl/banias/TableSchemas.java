package com.doitintl.banias;

import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableSchema;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

class TableSchemas {
	private static final String SCHEMA_FILTER_NAME = "Schema";
	private final HashMap<String, TableSchema> tableSchema;
	private static final String filesLocation = "./schemas/";
	private static final Logger LOG = LoggerFactory.getLogger(TableSchemas.class);

	public TableSchema getTableSchema(String key) {
		return tableSchema.get(key);
	}

	public TableSchemas() {
		tableSchema = new HashMap<>();
		File folder = new File(filesLocation);

		buildTableSchemaFromDatastore();

		for (String filename : Objects.requireNonNull(folder.list())) {
		    try{
		    	buildTableSchemaFromFile(filename);
			}catch (Exception iox){
				LOG.error(iox.toString());
			}
		}
	}

	private void buildTableSchemaFromDatastore(){
/*		Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
		KeyFactory keyFactory = datastore.newKeyFactory().setKind(SCHEMA_FILTER_NAME);
		IncompleteKey key = keyFactory.setKind(SCHEMA_FILTER_NAME).newKey();

		Query<Entity> query = Query
				.newEntityQueryBuilder()
				.setKind(SCHEMA_FILTER_NAME)
				.build();

		QueryResults<Entity> results = datastore.run(query);
		while (results.hasNext()) {
			Entity entity = results.next();
			String str = entity.toString();
			LOG.debug(str);
		}
*/	}

	private void buildTableSchemaFromFile(String fileName) throws IOException, ParseException {
		String fullName = filesLocation+fileName;
		String schemaKey = fileName.substring(0,fileName.indexOf(".json"));

		JSONParser parser = new org.json.simple.parser.JSONParser();
		JSONArray jsonSchema = (JSONArray) parser.parse(new FileReader(fullName));

		ArrayList<TableFieldSchema> fields = new ArrayList<>();
		for (Object field : jsonSchema) {
			if (field instanceof JSONObject) {
				fields.add(buildTableFieldSchemaFromJson((JSONObject) field));
			}
		}

		tableSchema.put(schemaKey,buildTableSchema(fields));
	}

	private TableFieldSchema buildTableFieldSchemaFromJson(JSONObject fieldJson) {
		if (fieldJson.containsKey("fields")) {
			List<TableFieldSchema> fields = new ArrayList<>();
			JSONArray fieldsJson = (JSONArray) fieldJson.get("fields");
			for (Object field : fieldsJson) {
				if (field instanceof JSONObject) {
					fields.add(buildTableFieldSchemaFromJson((JSONObject) field));
				}
			}
			return buildTableNestedFieldSchema(
					(String) fieldJson.get("name"), (String) fieldJson.get("mode"), fields);

		} else {
			return buildTableFieldSchema(
					(String) fieldJson.get("name"),
					(String) fieldJson.get("type"),
					(String) fieldJson.get("mode"));
		}
	}

	private TableSchema buildTableSchema(List<TableFieldSchema> fields) {
		TableSchema tableSchema = new TableSchema();
		tableSchema.setFields(fields);
		return tableSchema;
	}

	private TableFieldSchema buildTableFieldSchema(String name, String type, String mode) {
		TableFieldSchema fieldSchema = new TableFieldSchema();
		fieldSchema.setName(name).setType(type).setMode(mode);
		return fieldSchema;
	}

	private TableFieldSchema buildTableNestedFieldSchema(
			String name, String mode, List<TableFieldSchema> fields) {
		TableFieldSchema fieldSchema = new TableFieldSchema();
		fieldSchema.setName(name).setType("RECORD").setMode(mode).setFields(fields);
		return fieldSchema;
	}

}
