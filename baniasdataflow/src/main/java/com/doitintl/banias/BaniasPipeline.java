package com.doitintl.banias;

import com.google.api.services.bigquery.model.*;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.DynamicDestinations;
import org.apache.beam.sdk.io.gcp.bigquery.TableDestination;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.*;

import java.io.IOException;
import java.util.*;

public class BaniasPipeline {
	private static final TupleTag<TableRow> outputTag = new TupleTag<TableRow>() {};
	private static final TupleTag<TableRow> errorsTag = new TupleTag<TableRow>() {};
	private static final HashMap<String, TableSchema> tableSchema = new HashMap<>();

	static void updateTableSchema(String name, TableSchema tableSchema){
		tableSchema.put(name, tableSchema);
	}
	private static PCollection<TableRow> handleEvents (
			Pipeline pipeline, BaniasPipelineOptions options) {
		final String tableDestinationPrefix = options.getProject() + ":" + options.getDataset() + "." + options.getEventsTablePrefix() + "_";

		PCollectionTuple mappedEvents = pipeline
				.apply("Read Events from PubSub Messages", PubsubIO.readStrings().fromSubscription(options.getEventsSubscriptionPath()))
				.apply("Map Events", ParDo.of(new MapEvents(errorsTag, tableSchema, tableDestinationPrefix)).withOutputTags(outputTag, TupleTagList.of(errorsTag)));

		PCollection<TableRow> events = mappedEvents.get(outputTag);

		events.apply(BigQueryIO.<TableRow>write()
				.withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
				.withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
				.to(new DynamicDestinations<TableRow, String>() {
					@Override
					public String getDestination(ValueInSingleWindow<TableRow> event) {
						TableRow row = event.getValue();
						ArrayList<TableFieldSchema> fields = new ArrayList<>();
						String tableName = row.get("event_name").toString() + row.get("event_version").toString();

						try {
							Map<String, Object> keys = row.getUnknownKeys();

							keys.keySet().forEach(key -> fields.add(new TableFieldSchema().setName(key).setType("STRING")));
						}catch (NullPointerException npe){}

						tableSchema.put(tableName, new TableSchema().setFields(fields));
						return tableName;
					}

					@Override
					public TableDestination getTable(String tableName) {
						return new TableDestination(
								tableDestinationPrefix + tableName,
								"Table " + tableName);
					}

					@Override
					public TableSchema getSchema(String tableName) {
						return tableSchema.get(tableName);
					}
				})
				.withFormatFunction((SerializableFunction<TableRow, TableRow>) input -> input)
		);

		return mappedEvents.get(errorsTag);
	}

	public static void main(String[] args) throws IOException{
		PipelineOptionsFactory.register(BaniasPipelineOptions.class);

		BaniasPipelineOptions options = PipelineOptionsFactory
				.fromArgs(args)
				.withValidation()
				.as(BaniasPipelineOptions.class);

		options.setStreaming(true);

		Pipeline pipeline = Pipeline.create(options);

		PCollection<TableRow> errorEvents = handleEvents(pipeline, options);

		TableReference tableReference = new TableReference()
				.setProjectId(options.getProject())
				.setTableId(options.getErrorsTableName());

/*
		errorEvents.apply(
				"Write to BigQuery " + tableReference.toPrettyString() + " table",
				BigQueryIO
						.writeTableRows()
						.to(tableReference)
						.withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
						.withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
						.withFormatFunction((SerializableFunction<TableRow, TableRow>) input -> input)
		);

*/
		pipeline.run();
	}
}
