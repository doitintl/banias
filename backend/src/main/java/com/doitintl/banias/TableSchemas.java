package com.doitintl.banias;

import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableSchema;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

class TableSchemas implements Serializable {
	private static final String filesLocation= "./schemas/";
	private static final Logger LOG = LoggerFactory.getLogger(TableSchemas.class);
	private static HashMap<String, TableSchema> tableSchema = new HashMap<>();

	public TableSchemas() {
		loadFromLocalFile();
	}

	TableSchema getTableSchema(String key) {
		return tableSchema.get(key);
	}

	public String listTableSchema(){
		return "TableSchema contains now: " + tableSchema.keySet().toString();
	}

	@Override
	public int hashCode() {

		return Objects.hash(tableSchema);
	}

	void loadFromGCS(String gcsFileName, String bucketName){
		try{
			Storage storage = StorageOptions.getDefaultInstance().getService();
			String schemaKey = gcsFileName.substring(0,gcsFileName.indexOf(".json"));
			byte[] content = storage.readAllBytes(BlobId.of(bucketName, gcsFileName));
			String data = new String(content, UTF_8);

			if (tableSchema.get(schemaKey)==null){
				JSONParser parser = new org.json.simple.parser.JSONParser();
				JSONArray jsonSchema = (JSONArray) parser.parse(data);
				buildTableSchemaFromFile(jsonSchema, schemaKey);
			}
		}catch (Exception e){
			LOG.error(e.toString());
		}
	}

	void loadFromLocalFile() {
		File folder = new File(filesLocation);

		for (String fileName : Objects.requireNonNull(folder.list())) {
			try{
				String fullName = filesLocation+fileName;
				String schemaKey = fileName.substring(0,fileName.indexOf(".json"));
				JSONParser parser = new org.json.simple.parser.JSONParser();
				JSONArray jsonSchema = (JSONArray) parser.parse(new FileReader(fullName));

				buildTableSchemaFromFile(jsonSchema, schemaKey);
			}catch (Exception iox){
				LOG.error(iox.toString());
			}
		}
	}

	private void buildTableSchemaFromFile(JSONArray jsonSchema, String schemaKey){
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
