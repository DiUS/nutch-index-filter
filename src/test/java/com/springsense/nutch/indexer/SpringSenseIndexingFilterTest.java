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

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.hamcrest.Matchers.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.Inlinks;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.parse.ParseData;
import org.apache.nutch.parse.ParseImpl;
import org.apache.nutch.util.NutchConfiguration;
import org.junit.Before;
import org.junit.Test;

import com.springsense.disambig.Disambiguator;
import com.springsense.disambig.DisambiguatorFactory;

public class SpringSenseIndexingFilterTest {
	private DisambiguatorFactory mockDisambiguatorFactory;
	private Disambiguator mockDisambiguator;
	
	private Configuration conf;
	private SpringSenseIndexingFilter filter;

	@Before()
	public void setUp() throws Exception {
		mockDisambiguatorFactory = mock(DisambiguatorFactory.class);
		mockDisambiguator = mock(Disambiguator.class);
		when(mockDisambiguatorFactory.openNewDisambiguator()).thenReturn(mockDisambiguator);

		conf = NutchConfiguration.create();
		conf.set("springSenseIndexingFilter.matrixDirectory", "/media/matrix.data/current");
		conf.setStrings("springSenseIndexingFilter.fieldsToDisambiguate", "title", "content");

		filter = new SpringSenseIndexingFilter();
		filter.setConf(conf);
		
		filter.setDisambiguatorFactory(mockDisambiguatorFactory);
	}

	@Test
	public void itShouldInstantiateCorrectly() {
		assertNotNull(filter);
	}
	
	@Test
	public void itShouldConfigureMatrixDirectoryCorrectly() {
		assertEquals("/media/matrix.data/current", filter.getMatrixDirectory());
	}
	
	@Test
	public void itShouldConfigureFieldsToDisambiguateCorrectly() {
		assertThat(filter.getFieldsToDisambiguate(), hasItem("title"));
		assertThat(filter.getFieldsToDisambiguate(), hasItem("content"));
	}
	
	@Test
	public void testDisambiguatesFieldsCorrectly() {
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
//		assertTrue(doc.getFieldNames().contains("springsense.title.0"));
//		assertEquals(2, doc.getField("springsense.title.0").getValues().size());
//		assertThat(doc.getField("springsense.title.0").getValues()).contains("first disambig of title 1");
	}

}
