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
import java.util.HashSet;
import java.util.Set;

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

import com.springsense.disambig.DisambiguatorFactory;

public class SpringSenseIndexingFilter extends Configured implements IndexingFilter {
	public static final Logger LOG = LoggerFactory.getLogger(SpringSenseIndexingFilter.class);

	private DisambiguatorFactory disambiguatorFactory = null;

	private static DisambiguatorFactory classLoaderDisambiguationFactory = null;

	public NutchDocument filter(NutchDocument doc, Parse parse, Text url, CrawlDatum datum, Inlinks inlinks) throws IndexingException {

		return doc;
	}

	public String getMatrixDirectory() {
		return getConf().get("springSenseIndexingFilter.matrixDirectory");
	}

	public Set<String> getFieldsToDisambiguate() {
		return new HashSet<String>(getConf().getStringCollection("springSenseIndexingFilter.fieldsToDisambiguate"));
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

}
