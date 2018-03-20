/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.doitintl.banias;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;












public class BaniasPipeline {
    static class ExtractEventTypeFn extends DoFn<String, String> {
        private static final Logger LOG = LoggerFactory.getLogger(ExtractEventTypeFn.class);
        @ProcessElement
        public void processElement(ProcessContext c) {
            LOG.info(c.element());
            c.output(c.element());
        }
    }



    private static final Logger LOG = LoggerFactory.getLogger(BaniasPipeline.class);

    /**
     * Options supported by {@link PubSubToBigQuery}.
     */
    public interface Options extends PipelineOptions {
        @Description("Table spec to write the output to")
        ValueProvider<String> getOutputTableSpec();

        void setOutputTableSpec(ValueProvider<String> value);

        @Description("Pub/Sub topic to read the input from")
        ValueProvider<String> getInputTopic();

        void setInputTopic(ValueProvider<String> value);
    }


    public static void main(String[] args) {
        Options options = PipelineOptionsFactory.fromArgs(args).withValidation()
                .as(Options.class);
        Pipeline pipeline = Pipeline.create(options);
        final ValueProvider<String> tableSpec = options.getOutputTableSpec();
        pipeline
                .apply("ReadPubsub", PubsubIO.readStrings()
                        .fromTopic(options.getInputTopic()))
                .apply(BigQueryConverters.jsonToTableRow())
                .apply("WriteBigQuery", BigQueryIO.writeTableRows()
                        .withoutValidation()
                        .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_NEVER)
                        .to(options.getOutputTableSpec()));

        pipeline.run();

    }
}
