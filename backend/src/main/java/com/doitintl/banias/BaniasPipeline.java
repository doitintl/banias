package com.doitintl.banias;

import com.google.api.services.bigquery.model.TableReference;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.FileIO;
import org.apache.beam.sdk.io.fs.MatchResult.Metadata;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.DynamicDestinations;
import org.apache.beam.sdk.io.gcp.bigquery.TableDestination;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.transforms.Watch;
import org.apache.beam.sdk.values.*;
import org.joda.time.Duration;

import java.io.IOException;
import java.util.Objects;


class BaniasPipeline {
	private static TableSchemas tableSchemas = new TableSchemas();
	private static final TupleTag<TableRow> outputTag= new TupleTag<TableRow>() {};
	private static final TupleTag<TableRow> errorsTag = new TupleTag<TableRow>() {};

	private static PCollection<TableRow> handleEvents (Pipeline pipeline, BaniasPipelineOptions options) {
		final String tableDestinationPrefix = options.getProject() + ":" + options.getDataset() + ".";

		PCollectionTuple mappedEvents = pipeline
				.apply("Read Events from PubSub Messages", PubsubIO
						.readStrings()
						.fromSubscription(options.getEventsSubscriptionPath()))
				.apply("Map Events", ParDo.of(new MapEvents(errorsTag))
						.withOutputTags(outputTag, TupleTagList.of(errorsTag)));

		PCollection<TableRow> events = mappedEvents.get(outputTag);

		events.apply(BigQueryIO.<TableRow>write()
				.withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
				.withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
				.to(new DynamicDestinations<TableRow, String>() {
					@Override
					public String getDestination(ValueInSingleWindow<TableRow> event) {
						return parseDestination(Objects.requireNonNull(event.getValue()));
					}

					@Override
					public TableDestination getTable(String tableName) {
						return new TableDestination(
								tableDestinationPrefix + tableName,
								"Table " + tableName);
					}

					@Override
					public TableSchema getSchema(String tableName) {
						return tableSchemas.getTableSchema(tableName);
					}

					private String parseDestination(TableRow row){
						return row.get("event_name").toString() + "_" + row.get("event_version").toString();
					}
				})
				.withFormatFunction((SerializableFunction<TableRow, TableRow>) input -> {
					TableRow output = input.clone();
					output.remove("event_version");
					output.remove("event_name");
					return output;
				}));

		return mappedEvents.get(errorsTag);
	}

	private static void handleSchemas (Pipeline schemasPipeline, BaniasPipelineOptions options) {
        PCollection<Metadata> schemas = schemasPipeline
		    .apply("Read new schema files" ,FileIO.match()
				    .filepattern("gs://" + options.getGCSSchemasBucketName() + "/*")
				    .continuously(Duration.standardSeconds(30), Watch.Growth.never()));

		schemas.apply("Update new BQ schema", ParDo.of(new DoFn<Metadata, String>() {
			@ProcessElement
			public void processElement(ProcessContext processContext) {
				String out = processContext.element().resourceId().getFilename();
				tableSchemas.loadFromGCS(out);
				processContext.output(out);
			}
		}));

	}

	public static void main(String[] args) throws IOException{
		PipelineOptionsFactory.register(BaniasPipelineOptions.class);
		BaniasPipelineOptions eventsPipelineOptions = PipelineOptionsFactory
				.fromArgs(args)
				.withValidation()
				.as(BaniasPipelineOptions.class);
		eventsPipelineOptions.setStreaming(true);

		// Define pipeline
		Pipeline pipeline = Pipeline.create(eventsPipelineOptions);

		// Schemas handling
		TableSchemas.setBucketName(eventsPipelineOptions.getGCSSchemasBucketName());
		tableSchemas.loadFromLocalFile();
		handleSchemas(pipeline, eventsPipelineOptions);

		//Events handling
		PCollection<TableRow> errorEvents = handleEvents(pipeline, eventsPipelineOptions);

		TableReference tableRef = new TableReference()
				.setProjectId(eventsPipelineOptions.getProject())
				.setDatasetId(eventsPipelineOptions.getDataset())
				.setTableId(eventsPipelineOptions.getErrorsTableName());

		//Error handling
		errorEvents.apply("Write BigQuery " + tableRef.toPrettyString() + " table",
				BigQueryIO.writeTableRows().to(tableRef)
						.withSchema(tableSchemas.getTableSchema("errors"))
						.withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
						.withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND));

		pipeline.run();
	}
}
