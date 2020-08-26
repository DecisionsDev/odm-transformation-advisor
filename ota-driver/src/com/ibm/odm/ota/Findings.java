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
package com.ibm.odm.ota;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * Represents the table of possible findings, with template descriptions for
 * reporting. Also captures the type and characteristics of markers for each
 * finding.
 * 
 * @author pberland@us.ibm.com
 *
 */
public class Findings {

	private static Logger logger = Logger.getLogger(Findings.class.getName());

	public class Finding {
		public String description;
		public String title;
		public String summary;
		public List<String> flags;
	}

	public class Marker {
		public String text;
		public String color;
		public String icon;
		public int importance;
	}

	private Map<String, Finding> findings = new HashMap<String, Finding>();
	private Map<String, Marker> markers = new HashMap<String, Marker>();
	private Parameters parameters;
	private DataFormatter dataFormatter;

	private static Findings instance = null;

	private Findings() {
	}

	private static Findings getInstance() throws OTAException {
		if (instance == null) {
			instance = new Findings();
			instance.load("findings.xlsx");
		}
		return instance;
	}

	/**
	 * Loads the findings configuration file.
	 * 
	 * @param filename
	 * @throws OTAException
	 */
	private void load(String filename) throws OTAException {
		try {
			URI filepath = ClassLoader.getSystemResource(filename).toURI();
			logger.info("Loading findings configuration file " + filepath);
			Workbook workbook = WorkbookFactory.create(new File(filepath));
			dataFormatter = new DataFormatter();
			loadFindings(workbook.getSheet("findings"));
			loadMarkers(workbook.getSheet("markers"));
			// Workbook.close is not available in older POI versions.
			// workbook.close();
		}
		// WorkbookFactory.create throws InvalidFormatException in older POI versions.
		// catch (URISyntaxException | IOException e) {
		catch (Exception e) {
			throw new OTAException("Error loading findings configuration file", e);
		}
	}

	/**
	 * Loads each row of the findings sheet. The value of predefined cells is not
	 * checked and assumed to be correct.
	 * 
	 * @param sheet
	 * @throws OTAException
	 */
	private void loadFindings(Sheet sheet) throws OTAException {
		boolean skippedFirst = false;
		parameters = new Parameters();
		for (Row row : sheet) {
			if (skippedFirst) {
				String tag = getCell(row, 0);
				Finding finding = new Finding();
				finding.description = getCell(row, 1);
				finding.summary = getCell(row, 2);
				finding.title = getCell(row, 4);
				finding.flags = splitFlagString(getCell(row, 3));
				findings.put(tag, finding);
				addParamsString(getCell(row, 7));
			} else {
				skippedFirst = true;
			}
		}
	}

	private List<String> splitFlagString(String flagString) {
		return Arrays.asList(flagString.trim().split("\\s*,\\s*"));
	}

	/**
	 * Add the parameters associated with the finding if any are defined.
	 * 
	 * @param paramsString
	 * @return
	 * @throws OTAException
	 */
	private void addParamsString(String paramsString) throws OTAException {
		paramsString = paramsString.trim();
		if (!paramsString.isEmpty()) {
			final String paramsPattern = "(\\s*[a-z][a-zA-Z0-9]*\\s*=\\s*[a-zA-Z0-9]*\\s*,)*\\s*[a-z][a-zA-Z0-9]*\\s*=\\s*[a-zA-Z0-9]*\\s*";
			if (!paramsString.matches(paramsPattern)) {
				throw new OTAException("Invalid finding parameters definition: " + paramsString);
			}
			String[] assignments = paramsString.split("\\s*,\\s*");
			for (String assignment : assignments) {
				String[] members = assignment.split("\\s*=\\s*");
				parameters.put(members[0], members[1]);
			}
		}
	}

	private void loadMarkers(Sheet sheet) {
		boolean skippedFirst = false;
		for (Row row : sheet) {
			if (skippedFirst) {
				String tag = getCell(row, 0);
				Marker marker = new Marker();
				marker.text = getCell(row, 1);
				marker.color = getCell(row, 2);
				marker.icon = getCell(row, 3);
				marker.importance = Integer.parseInt(getCell(row, 4));
				markers.put(tag, marker);
			} else {
				skippedFirst = true;
			}
		}
	}

	private String getCell(Row row, int index) {
		return dataFormatter.formatCellValue(row.getCell(index));
	}

	public static int getFindingImportance(String tag) {
		int importance = 0;
		for (String flag : instance.findings.get(tag).flags) {
			importance += instance.markers.get(flag).importance;
		}
		return importance;
	}

	public static List<String> getFindings() throws OTAException {
		return new ArrayList<String>(getInstance().findings.keySet());
	}

	public static Finding getFinding(String tag) throws OTAException {
		return getInstance().findings.get(tag);
	}

	public static List<String> getMarkers() throws OTAException {
		return new ArrayList<String>(getInstance().markers.keySet());
	}

	public static Marker getMarker(String tag) throws OTAException {
		return getInstance().markers.get(tag);
	}

	public static Parameters getParameters() throws OTAException {
		return getInstance().parameters;
	}
}
