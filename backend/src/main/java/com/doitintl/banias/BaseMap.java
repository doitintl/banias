package com.doitintl.banias;

import com.google.api.services.bigquery.model.TableRow;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.TupleTag;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class BaseMap extends DoFn<String, TableRow> {
	private static final Logger LOG = LoggerFactory.getLogger(BaseMap.class);
	private static final long serialVersionUID = -7252861063735099405L;
	private TupleTag<TableRow> errorsTag;

	BaseMap(TupleTag<TableRow> errorsTag) {
		this.errorsTag = errorsTag;
	}

	abstract String getType();

	abstract void map(JSONObject json, TableRow tableRow);

	@ProcessElement
	public void processElement(ProcessContext processContext) {
		TableRow tableRow = new TableRow();

		try {
			JSONObject json = new JSONObject(processContext.element());

			JSONObject eventJson = json.getJSONObject("Event");
			JSONObject typeJson = eventJson.getJSONObject("type");
			JSONObject payloadJson = eventJson.getJSONObject("payload");

			tableRow.set("SenderID", json.getString("SenderID"));
			tableRow.set("event_version", typeJson.getString("event_version"));
			tableRow.set("event_name", typeJson.getString("event_name"));

			map(payloadJson, tableRow);
			processContext.output(tableRow);

		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			tableRow.clear();
			tableRow.set(SchemaHelpers.ERROR_EVENT_TYPE_STR, getType());
			tableRow.set(SchemaHelpers.ERROR_EVENT_RAW_INPUT_STR, processContext.element());
			tableRow.set(SchemaHelpers.ERROR_EVENT_ERROR_STR, e.toString());
			processContext.output(errorsTag, tableRow);
		}
	}
}
