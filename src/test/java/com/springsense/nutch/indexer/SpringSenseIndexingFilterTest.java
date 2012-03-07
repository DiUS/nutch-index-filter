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

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.Inlinks;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.indexer.NutchField;
import org.apache.nutch.parse.ParseData;
import org.apache.nutch.parse.ParseImpl;
import org.apache.nutch.util.NutchConfiguration;
import org.junit.Before;
import org.junit.Test;

public class SpringSenseIndexingFilterTest {

	private Configuration conf;
	private SpringSenseIndexingFilter filter;
	private NutchDocument nutchDocument;
	private ParseImpl parse;

	@Before()
	public void setUp() throws Exception {
		conf = NutchConfiguration.create();
		conf.set("springSenseIndexingFilter.matrixDirectory", "/media/matrix.data/current/this");
		conf.setStrings("springSenseIndexingFilter.fieldsToDisambiguate", "title", "content");

		filter = new SpringSenseIndexingFilter();
		filter.setConf(conf);
		
		nutchDocument = new NutchDocument();
		nutchDocument.add("title", "title 1");
		nutchDocument.add("title", "title 2");
		nutchDocument.add("content", "content 1");

		parse = new ParseImpl("Not used", new ParseData());
	}

	@Test
	public void itShouldInstantiateCorrectly() {
		assertNotNull(filter);
	}
	
	@Test
	public void itShouldConfigureMatrixDirectoryCorrectly() {
		assertEquals("/media/matrix.data/current/this", filter.getMatrixDirectory());
	}
	
	@Test
	public void itShouldConfigureFieldsToDisambiguateCorrectly() {
		assertThat(filter.getFieldsToDisambiguate(), hasItem("title"));
		assertThat(filter.getFieldsToDisambiguate(), hasItem("content"));
	}
	
	@Test
	public void itShouldDisambiguateFieldsCorrectly() {
		try {
			filter.filter(nutchDocument, parse, new Text("http://nutch.apache.org/index.html"), new CrawlDatum(), new Inlinks());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		assertNotNull(nutchDocument);

		assertTrue(nutchDocument.getFieldNames().contains("springsense.title.text.0"));
		assertTrue(nutchDocument.getFieldNames().contains("springsense.content.text.0"));
		
		NutchField firstVariantTitleField = nutchDocument.getField("springsense.title.text.0");
		NutchField firstVariantContentField = nutchDocument.getField("springsense.content.text.0");
		
		assertEquals(2, firstVariantTitleField.getValues().size());
		assertEquals(1, firstVariantContentField.getValues().size());
		
		assertThat(firstVariantTitleField.getValues(), hasItem((Object)"title_n_01 1"));
		assertThat(firstVariantTitleField.getValues(), hasItem((Object)"title_n_01 2"));

		assertThat(firstVariantContentField.getValues(), hasItem((Object)"content_n_01 1"));
	}

}
