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
import java.util.Hashtable;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

class SchemaHelpers implements Serializable {
	private static final Logger LOG = LoggerFactory.getLogger(SchemaHelpers.class);
	private static final long serialVersionUID = 4048366826024870134L;

	static Hashtable<String,TableSchema> loadSchemaFromGCS(String bucketName){
		Hashtable<String,TableSchema> schemas = new Hashtable<>();
		try{
			Storage storage = StorageOptions.getDefaultInstance().getService();
			Bucket bucket = storage.get(bucketName);

			bucket.list().iterateAll().forEach(blob -> {
				String gcsFileName = blob.getName();
				String key = gcsFileName.substring(0,gcsFileName.indexOf(".json"));

				TableSchema val = fromJsonString(new String(storage.readAllBytes(blob.getBlobId()), UTF_8));

				schemas.put(key, val);
			});
		}catch (Exception e){
			LOG.error(e.toString());
		}

		return schemas;
	}

	private static TableSchema fromJsonString(String json) {
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
		fields.add(new TableFieldSchema().setName("type").setType("STRING").setMode("NULLABLE"));
		fields.add(new TableFieldSchema().setName("raw_input").setType("STRING").setMode("NULLABLE"));

		TableSchema tableSchema = new TableSchema();
		tableSchema.setFields(fields);
		return tableSchema;
	}
}
