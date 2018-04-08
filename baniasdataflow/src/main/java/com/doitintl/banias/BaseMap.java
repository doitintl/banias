package com.doitintl.banias;

import com.google.api.services.bigquery.model.TableRow;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.TupleTag;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class BaseMap extends DoFn<String, TableRow> {
	protected static final Logger LOG = LoggerFactory.getLogger(MapEvents.class);
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

			if (json.has("SenderID")) {
				tableRow.set("SenderID", json.get("SenderID").toString());
			}
			map(json, tableRow);
			processContext.output(tableRow);
		} catch (Exception e) {
			LOG.error(e.toString());
			tableRow.set("type", getType());
			tableRow.set("payload", processContext.element());
			processContext.output(errorsTag, tableRow);
		}
	}
}
