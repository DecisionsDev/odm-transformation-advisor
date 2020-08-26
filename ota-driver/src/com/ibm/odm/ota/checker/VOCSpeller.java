/*
* Copyright IBM Corp. 1987, 2018
* 
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
* 
* http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
* 
**/
package com.ibm.odm.ota.checker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.ibm.odm.ota.OTAException;

/**
 * Basic dictionary, used to check BOM verbalizations. TODO: allow for multiple
 * languages (select based on DC repository locale?)
 * 
 * English dictionary credit: http://wordlist.aspell.net/12dicts-readme/
 * 
 * @author pberland@us.ibm.com
 *
 */
public class VOCSpeller {

	private boolean ready = false;
	private HashSet<String> validWords = new HashSet<String>();
	private HashSet<String> flaggedWords = new HashSet<String>();

	private static VOCSpeller speller = null;

	private static final String DICT_FILE = "spelling.txt";
	private static Logger logger = Logger.getLogger(VOCSpeller.class.getCanonicalName());

	public boolean isReady() {
		return ready;
	}

	/**
	 * Returns the list of misspelled words in the given verbalization string, or
	 * null if no dictionary is available.
	 * 
	 * @param verbalization
	 * @return
	 */
	public List<String> getMisspelled(String verbalization) {
		List<String> flagged = null;
		if (ready) {
			flagged = new ArrayList<String>();
			Set<String> words = breakupVerbalization(verbalization);
			for (String word : words) {
				if (!isValidOrAlreadyFlagged(word)) {
					flagged.add(word);
				}
			}
		}
		return flagged;
	}

	public static VOCSpeller getSpeller() throws OTAException {
		if (speller == null) {
			speller = new VOCSpeller();
		}
		return speller;
	}

	private VOCSpeller() throws OTAException {
		try {
			URL dictURL = ClassLoader.getSystemResource(DICT_FILE);
			if (dictURL == null) {
				logger.warning(
						"Cannot find the spelling dictionary file 'spelling.txt' in the resources folder. Spell check will not be performed.");
				this.ready = false;
				return;
			}
			Path cssPath = Paths.get(dictURL.toURI());
			BufferedReader is = new BufferedReader(new FileReader(cssPath.toFile().getAbsolutePath()));
			String word;
			while ((word = is.readLine()) != null) {
				validWords.add(word.trim());
			}
			is.close();
			this.ready = true;
		} catch (URISyntaxException | IOException e) {
			throw new OTAException("Error loading the spelling dictionary", e);
		}
	}

	/**
	 * Maintains overall list flagged words to flag them only once.
	 * 
	 * @param word
	 * @return
	 */
	private boolean isValidOrAlreadyFlagged(String word) {
		if (validWords.contains(word) || flaggedWords.contains(word)) {
			return true;
		}
		flaggedWords.add(word);
		return false;
	}

	/**
	 * Breaks-up the given verbalization into a set of words.
	 * 
	 * @param verbalization
	 * @return
	 */
	private Set<String> breakupVerbalization(String verbalization) {
		Set<String> breakup = new HashSet<String>();
		//
		// Remove the parameters placeholders.
		//
		String[] chunks = verbalization.split("\\{[^\\}]*\\}");
		for (String chunk : chunks) {
			String alphaChunk = chunk.replaceAll("[^a-zA-Z ]", " ");
			String[] words = alphaChunk.trim().split(" +");
			for (String word : words) {
				// Remove:
				// - empty strings
				// - all-caps words, which have a chance to be an acronym
				// - words of length 1
				if (!word.isEmpty() && (word.length() > 1) && !(word.equals(word.toUpperCase()))) {
					breakup.add(word.toLowerCase());
				}
			}
		}
		return breakup;
	}

}
