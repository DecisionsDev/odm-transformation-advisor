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

import ilog.rules.teamserver.model.permalink.IlrPermanentLinkHelper;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import com.ibm.icu.text.DateFormat;

/**
 * Formats and outputs report to HTML file.
 * 
 * @author pberland@us.ibm.com.
 *
 */
public class ReportFormatter implements Comparator<String> {
	private static final String CSS_FILE = "report.css";
	private static final String HTML_TMPL_FILE = "report.html";
	private static final String PREFERENCES_FILE = "preferences.properties";

	private static final String SUMMARY_TMPL = "<div><p class=\"summary-head\">%s</p></div>\n<div><p class=\"summary-body\">%s</p></div>\n<br>\n";
	private static final String MARKERS_TMPL = "&nbsp;<span style=\"color:%s;font-size:20px\">%s;</span>";
	private static final String CELL_TMPL = "<td>%s</td>\n";

	private PrintWriter os = null;
	private Properties preferences = new Properties();
	private int notableItems = 0;
	private IlrPermanentLinkHelper permalinkHelper;

	private enum Context {
		summary, detail;
	}

	public ReportFormatter() throws OTAException {
		try {
			permalinkHelper = new IlrPermanentLinkHelper(
					DCConnection.getSession());
			preferences.load(ClassLoader
					.getSystemResourceAsStream(PREFERENCES_FILE));
		} catch (IOException e) {
			throw new OTAException(
					"Error loading report formatter preference file", e);
		}
	}

	public void createHTML(Report report, String filename) throws OTAException {
		try {
			Path htmlPath = Paths.get(ClassLoader.getSystemResource(
					HTML_TMPL_FILE).toURI());
			String template = new String(Files.readAllBytes(htmlPath));
			template = template.replace("$timestamp", getTimestamp());
			template = template.replace("$username", report.getUsername());
			template = template.replace("$repository", report.getUrl());
			template = template.replace("$datasource", report.getDatasource());
			template = template.replace("$css", getCSS());
			template = template.replace("$summaries", getSummaries(report));
			template = template.replace("$impact", getImpact());
			template = template.replace("$details", getDetails(report));
			template = template.replace("$items",
					Integer.toString(notableItems));

			os = new PrintWriter(new FileWriter(filename));
			os.print(template);
			os.close();
		} catch (URISyntaxException | IOException e) {
			throw new OTAException("Error loading report template file", e);
		}
	}

	private String getTimestamp() {
		DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.FULL,
				DateFormat.SHORT);
		return dateFormat.format(new Date());
	}

	private String getCSS() throws OTAException {
		try {
			Path cssPath = Paths.get(ClassLoader.getSystemResource(CSS_FILE)
					.toURI());
			return new String(Files.readAllBytes(cssPath));
		} catch (IOException | URISyntaxException e) {
			throw new OTAException("Error loading CSS file", e);
		}
	}

	private String getSummaries(Report report) throws OTAException {
		String summaries = "";
		notableItems = 0;

		List<String> findings = Findings.getFindings();
		Collections.sort(findings, this);

		for (String type : findings) {
			List<ReportElement> elementsFound = new ArrayList<ReportElement>();
			for (ReportElement element : report.getElements()) {
				if (element.getType().equals(type)
						&& isReportable(element, Context.summary)) {
					elementsFound.add(element);
				}
			}
			int count = elementsFound.size();
			if (count > 0) {
				ReportElement selected = elementsFound.get(0);
				String total = Integer.toString(count);
				String plural = (count > 1) ? "s" : "";
				String title = Findings.getFinding(type).description;
				String format = Findings.getFinding(type).summary;
				String summaryBody = String.format(format, selected.getWhere(),
						selected.getWhat(), total, plural);
				String summaryHead = title + getMarkers(type);
				summaries += String.format(SUMMARY_TMPL, summaryHead,
						summaryBody);
				notableItems++;
			}
		}
		return summaries;
	}

	private String getMarkers(String type) throws OTAException {
		String result = "";
		for (String flag : Findings.getFinding(type).flags) {
			if (isReportable(flag, Context.summary)) {
				String color = Findings.getMarker(flag).color;
				String icon = Findings.getMarker(flag).icon;
				String markerString = String.format(MARKERS_TMPL, color, icon);
				result += markerString;
			}
		}
		return result;
	}

	private String getImpact() throws OTAException {
		String impact = "";
		for (String tag : Findings.getMarkers()) {
			if (isReportable(tag, Context.detail)) {
				String text = Findings.getMarker(tag).text;
				impact += "<th>" + text + "</th>" + "\n";
			}
		}
		return impact;
	}

	private String getDetails(Report report) throws OTAException {
		String details = "";
		for (ReportElement element : report.getElements()) {
			if (isReportable(element, Context.detail)) {
				details += "<tr>\n";
				details += String.format(CELL_TMPL,
						Findings.getFinding(element.getType()).title);
				details += String.format(CELL_TMPL, element.getWhere());
				String what = element.getWhat();
				if (element.getElement() != null) {
					String url = permalinkHelper.getElementDetailsURL(
							element.getWhere(), element.getElement());
					what = "<a href=\"" + url + "\">" + what + "</a>";
				}
				details += String.format(CELL_TMPL, what);
				List<String> elementFlags = Findings.getFinding(element
						.getType()).flags;
				for (String tag : Findings.getMarkers()) {
					if (isReportable(tag, Context.detail)) {
						String marked = elementFlags.contains(tag) ? "Y" : "";
						details += String.format(CELL_TMPL, marked);
					}
				}
				details += "</tr>\n";
			}
		}
		return details;
	}

	private boolean isReportable(ReportElement element, Context context)
			throws OTAException {
		for (String flag : Findings.getFinding(element.getType()).flags) {
			if (isReportable(flag, context)) {
				return true;
			}
		}
		return false;
	}

	private boolean isReportable(String flag, Context context) {
		String property = flag + ".report." + context.toString();
		return Boolean.valueOf(preferences.getProperty(property, "true"));
	}

	@Override
	public int compare(String tag1, String tag2) {

		int val1 = Findings.getFindingImportance(tag1);
		int val2 = Findings.getFindingImportance(tag2);
		return (val1 < val2) ? 1 : ((val1 > val2) ? -1 : 0);
	}

}
