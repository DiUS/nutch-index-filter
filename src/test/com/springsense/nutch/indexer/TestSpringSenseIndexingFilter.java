/**
 * Based on TestMoreIndexingFilter, licensed to the Apache Software Foundation (ASF) under one or more
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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.Inlinks;
import org.apache.nutch.indexer.IndexingException;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.metadata.Metadata;
import org.apache.nutch.net.protocols.Response;
import org.apache.nutch.parse.Outlink;
import org.apache.nutch.parse.ParseData;
import org.apache.nutch.parse.ParseImpl;
import org.apache.nutch.parse.ParseStatus;
import org.apache.nutch.util.NutchConfiguration;

import junit.framework.TestCase;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestSpringSenseIndexingFilter extends TestCase {

	public void testDisambiguatesFieldsCorrectly() {
		Configuration conf = NutchConfiguration.create();
		conf.setStrings("springSenseIndexingFilter.fieldsToDisambiguate", "title", "content");

		SpringSenseIndexingFilter filter = new SpringSenseIndexingFilter();
		filter.setConf(conf);
		assertNotNull(filter);

		NutchDocument doc = new NutchDocument();
		doc.add("title", "title 1");
		doc.add("title", "title 2");
		doc.add("content", "content 1");

		ParseImpl parse = new ParseImpl("Not used", new ParseData());

		try {
			filter.filter(doc, parse, new Text("http://nutch.apache.org/index.html"), new CrawlDatum(), new Inlinks());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		assertNotNull(doc);
		assertTrue(doc.getFieldNames().contains("springsense.title.0"));
		assertEquals(2, doc.getField("springsense.title.0").getValues().size());
		assertThat(doc.getField("springsense.title.0").getValues()).contains("first disambig of title 1");
	}

}
