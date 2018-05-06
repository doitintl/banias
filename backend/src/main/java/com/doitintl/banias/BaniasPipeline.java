package com.doitintl.banias;

import com.google.api.services.bigquery.model.TableReference;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.bigquery.DynamicDestinations;
import org.apache.beam.sdk.io.gcp.bigquery.TableDestination;
import org.apache.beam.sdk.io.gcp.bigquery.TableRowJsonCoder;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

class BaniasPipeline {
	private static final Logger LOG = LoggerFactory.getLogger(BaniasPipeline.class);
	private static final TupleTag<TableRow> outputTag= new TupleTag<>();
	private static final TupleTag<TableRow> errorsTag = new TupleTag<>();

	public static void main(String[] args){
		ConcurrentHashMap<String,String> schemas;

		PipelineOptionsFactory.register(BaniasPipelineOptions.class);

		BaniasPipelineOptions pipelineOptions = PipelineOptionsFactory
				.fromArgs(args)
				.withValidation()
				.as(BaniasPipelineOptions.class);

		pipelineOptions.setStreaming(true);

		String tableDestinationPrefix = pipelineOptions.getProject() + ":" + pipelineOptions.getDataset() + ".";
		schemas = SchemaHelpers.loadSchemaFromGCS(pipelineOptions.getGCSSchemasBucketName());

		// Define pipeline
		Pipeline pipeline = Pipeline.create(pipelineOptions);

		//Events handling
		PCollectionTuple mappedEvents = pipeline
				.apply("Read Events from PubSub Messages", PubsubIO
						.readStrings()
						.fromSubscription(pipelineOptions.getEventsSubscriptionPath()))
				.apply("Map Events", ParDo.of(new MapEvents(errorsTag))
						.withOutputTags(outputTag, TupleTagList.of(errorsTag)));

		PCollection<TableRow> events = mappedEvents.get(outputTag);

	    events.apply(
	        "Write To Dynamic Table on BQ",
	        BigQueryIO.<TableRow>write()
	            .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
	            .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
	            .to(
	                new EventDestinations(schemas, tableDestinationPrefix))
	            .withFormatFunction(
	                (SerializableFunction<TableRow, TableRow>)
	                    input -> {
	                	TableRow output = input.clone();
	                	output.remove("event_version");
	                	output.remove("event_name");
	                	return output;
	                }));

		PCollection<TableRow> errors = mappedEvents.get(errorsTag);

		//Error handling
		TableReference tableRef = new TableReference()
				.setProjectId(pipelineOptions.getProject())
				.setDatasetId(pipelineOptions.getDataset())
				.setTableId(pipelineOptions.getErrorsTableName());

		errors.setCoder(TableRowJsonCoder.of());
		errors.apply("Write Errors to BigQuery",
				BigQueryIO.writeTableRows().to(tableRef)
						.withSchema(SchemaHelpers.getErrorTableSchema())
						.withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
						.withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND));

		pipeline.run();
	}

	private static class EventDestinations extends DynamicDestinations<TableRow, String> {
		private final ConcurrentHashMap<String,String> schemas;
		private final String tableDestinationPrefix;

		private EventDestinations(ConcurrentHashMap<String,String> schemas, String tableDestinationPrefix) {
			this.schemas = schemas;
			this.tableDestinationPrefix = tableDestinationPrefix;
		}

		private static final long serialVersionUID = -2839237244568662696L;

		@Override
		public String getDestination(ValueInSingleWindow<TableRow> event) {
			if (event.getValue() == null)
				return "";
			return parseDestination(event.getValue());
		}

		@Override
		public TableDestination getTable(String tableName) {
			return new TableDestination(
					tableDestinationPrefix + tableName, "Table " + tableName);
		}

		@Override
		public TableSchema getSchema(String tableName) {
			return SchemaHelpers.fromJsonString(schemas.get(tableName));
		}

		private String parseDestination(TableRow row) {
			return row.get("event_name").toString()
					+ "_"
					+ row.get("event_version").toString();
		}
	}
}
