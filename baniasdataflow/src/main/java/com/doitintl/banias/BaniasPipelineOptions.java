package com.doitintl.banias;

import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.Validation;

public interface BaniasPipelineOptions extends DataflowPipelineOptions {
	@Description("Full path for 'events' PubSub subscription to accept messages from. Required. Format: projects/<project_id>/subscriptions/<subscription_id>")
	@Validation.Required
	String getEventsSubscriptionPath();
	void setEventsSubscriptionPath(String value);

/*
	@Description("Full path for 'control' PubSub subscription to accept messages from. Required. Format: projects/<project_id>/subscriptions/<subscription_id>")
	@Validation.Required
	String getControlSubscriptionPath();
	void setControlSubscriptionPath(String value);
*/

	@Description("BigQuery events table Prefix.")
	String getEventsTablePrefix();
	void setEventsTablePrefix(String value);

	@Description("BigQuery errors table name.")
	@Validation.Required
	String getErrorsTableName();
	void setErrorsTableName(String value);

	@Description("List of BiqQuery dataset name.")
	@Validation.Required
	String getDataset();
	void setDataset(String value);
}
