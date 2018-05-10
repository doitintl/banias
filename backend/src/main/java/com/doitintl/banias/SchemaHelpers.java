package com.doitintl.banias;

import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableSchema;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.apache.beam.sdk.util.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

class SchemaHelpers implements Serializable {
	private static final Logger LOG = LoggerFactory.getLogger(SchemaHelpers.class);
	private static final long serialVersionUID = 4048366826024870134L;

	static final String ERROR_EVENT_TYPE_STR = "type";
	static final String ERROR_EVENT_RAW_INPUT_STR = "raw_input";
	static final String ERROR_EVENT_ERROR_STR = "error";
	static final String ERROR_EVENT_DONT_HAVE_SCHEMA = "Event don't have a predefined schema";

	static ConcurrentHashMap<String,String> loadSchemaFromGCS(String bucketName){
		ConcurrentHashMap<String,String> schemas = new ConcurrentHashMap<>();
		try{
			Storage storage = StorageOptions.getDefaultInstance().getService();
			Bucket bucket = storage.get(bucketName);

			bucket.list().iterateAll().forEach(blob -> {
				String gcsFileName = blob.getName();
				String key = gcsFileName.substring(0,gcsFileName.indexOf(".json"));

				String val = new String(storage.readAllBytes(blob.getBlobId()), UTF_8);

				schemas.put(key, val);
			});
		}catch (Exception e){
			LOG.error(e.toString());
		}

		return schemas;
	}

	static TableSchema fromJsonString(String json) {
		if (json == null) {
			return null;
		}
		try {
			return Transport.getJsonFactory().fromString(json, TableSchema.class);
		} catch (IOException e) {
			throw new RuntimeException(
					String.format("Cannot deserialize %s from a JSON string: %s.", TableSchema.class, json), e);
		}
	}

	static TableSchema getErrorTableSchema(){
		List<TableFieldSchema> fields = new ArrayList<>();
		fields.add(new TableFieldSchema().setName(ERROR_EVENT_TYPE_STR).setType("STRING").setMode("NULLABLE"));
		fields.add(new TableFieldSchema().setName(ERROR_EVENT_RAW_INPUT_STR).setType("STRING").setMode("NULLABLE"));
		fields.add(new TableFieldSchema().setName(ERROR_EVENT_ERROR_STR).setType("STRING").setMode("NULLABLE"));

		TableSchema tableSchema = new TableSchema();
		tableSchema.setFields(fields);
		return tableSchema;
	}
}
