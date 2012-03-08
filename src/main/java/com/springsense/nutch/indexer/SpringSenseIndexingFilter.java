/**
 * Based on MoreIndexingFilter, licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.springsense.nutch.indexer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.Text;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.Inlinks;
import org.apache.nutch.indexer.IndexingException;
import org.apache.nutch.indexer.IndexingFilter;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.parse.Parse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.springsense.disambig.DisambiguationResult;
import com.springsense.disambig.DisambiguationResult.Sentence;
import com.springsense.disambig.DisambiguationResult.Variant;
import com.springsense.disambig.Disambiguator;
import com.springsense.disambig.DisambiguatorFactory;
import com.springsense.disambig.SentenceDisambiguationResult;

public class SpringSenseIndexingFilter extends Configured implements IndexingFilter {
	public static final Logger LOG = LoggerFactory.getLogger(SpringSenseIndexingFilter.class);

	private ThreadLocal<Disambiguator> disambiguatorStore;
	private DisambiguatorFactory disambiguatorFactory = null;

	private static DisambiguatorFactory classLoaderDisambiguationFactory = null;

	public NutchDocument filter(NutchDocument document, Parse parse, Text url, CrawlDatum datum, Inlinks inlinks) throws IndexingException {
		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("About to process '%s' with the SpringSense Indexing Filter. Fields to be disambiguated: %s", url.toString(), StringUtils.join(getFieldsToDisambiguate(), ',')));
		}
		
		for (String fieldToDisambiguate : getFieldsToDisambiguate()) {
			disambiguateAndStore(document, fieldToDisambiguate);
		}
		LOG.debug("\tDone.");
		
		return document;
	}

	public String getMatrixDirectory() {
		return getConf().get("springSenseIndexingFilter.matrixDirectory");
	}

	public Set<String> getFieldsToDisambiguate() {
		String[] split = getConf().get("springSenseIndexingFilter.fieldsToDisambiguate").split("\\s*,\\s*");
		
		return new HashSet<String>(Arrays.asList(split));
	}

	protected DisambiguationResult convertToApiView(SentenceDisambiguationResult[] result) {
		List<Sentence> sentences = new ArrayList<Sentence>(result == null ? 0 : result.length);
		if (result != null) {
			for (SentenceDisambiguationResult taggedSentence : result) {
				sentences.add(taggedSentence.toApiView());
			}
		}

		DisambiguationResult resultAsApi = new DisambiguationResult(sentences);
		return resultAsApi;
	}

	protected void disambiguateAndStore(NutchDocument document, String fieldName) {
		if (!document.getFieldNames().contains(fieldName)) {
			return;
		}

		String springSenseTextFieldName = String.format("springsense.%s.text", fieldName);

		List<Object> fieldValue = document.getField(fieldName).getValues();
		for (Object valueObj : fieldValue) {
			
			String value = valueObj.toString();
			SentenceDisambiguationResult[] result = getDisambiguator().disambiguateText(value, 3, false, true, false);
			DisambiguationResult resultAsApi = convertToApiView(result);

			int i = 0;
			List<Variant> variants = resultAsApi.getVariants();
			for (Variant variant : variants) {
				document.add(String.format("%s.%d", springSenseTextFieldName, i), variant.toString());
				i++;
			}
		}
	}	
	
	public DisambiguatorFactory getDisambiguatorFactory() {
		if (disambiguatorFactory == null) {
			synchronized (getClass()) {
				if (classLoaderDisambiguationFactory == null) {
					// Get the jvm heap size.
					long heapSize = Runtime.getRuntime().totalMemory();

					// Print the jvm heap size.
					LOG.info(String.format("Heap Size: %d", heapSize));

					LOG.info(String.format("Starting new disambiguator factory with path '%s'", getMatrixDirectory()));
					try {
						classLoaderDisambiguationFactory = new DisambiguatorFactory(getMatrixDirectory());
					} catch (IOException e) {
						throw new RuntimeException("Failed to create new disambiguation factory due to an error", e);
					}
					LOG.debug("Done.");
				}
			}

			disambiguatorFactory = classLoaderDisambiguationFactory;
		}
		return disambiguatorFactory;
	}

	protected void setDisambiguatorFactory(DisambiguatorFactory disambiguatorFactory) {
		this.disambiguatorFactory = disambiguatorFactory;
	}

	public Disambiguator getDisambiguator() {
		return ((Disambiguator) (this.getDisambiguatorStore().get()));
	}

	public ThreadLocal<Disambiguator> getDisambiguatorStore() {
		if (disambiguatorStore == null) {
			disambiguatorStore = new ThreadLocal<Disambiguator>() {

				@Override
				protected Disambiguator initialValue() {
					try {
						LOG.info("Opening a new disambiguator for this thread.");
						return (getDisambiguatorFactory().openNewDisambiguator());
					} catch (IOException e) {
						throw new RuntimeException("Could not create new disambiguator due to an error", e);
					}
				}

			};
		}
		return disambiguatorStore;
	}

	
}
