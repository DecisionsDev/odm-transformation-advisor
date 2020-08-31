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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.emf.ecore.EClass;

import com.ibm.odm.ota.DCConnection;
import com.ibm.odm.ota.OTAException;
import com.ibm.odm.ota.ProjectSelections;
import com.ibm.odm.ota.ProjectSelections.Item;
import com.ibm.odm.ota.Report;

import ilog.rules.teamserver.brm.IlrBOM;
import ilog.rules.teamserver.brm.IlrBOM2XOMMapping;
import ilog.rules.teamserver.brm.IlrRuleProject;
import ilog.rules.teamserver.brm.IlrVocabulary;
import ilog.rules.teamserver.model.IlrDefaultSearchCriteria;
import ilog.rules.teamserver.model.IlrElementDetails;
import ilog.rules.teamserver.model.IlrObjectNotFoundException;
import ilog.rules.teamserver.model.IlrSession;
import ilog.rules.teamserver.model.permissions.IlrRoleRestrictedPermissionException;

/**
 * Performs BOM checks on textual versions using mostly string patterns (so
 * these checks will not 100% reliable).
 * 
 * @author pberland@us.ibm.com
 *
 */
public class BOMChecker extends Checker {

	private IlrRuleProject bomHolder;
	private List<String> visited = new ArrayList<String>();

	private enum BOMComponent {
		BOM, B2X, VOC
	}

	private static final String VALUE_INFO1_PATTERN = "property *valueInfo *\"(.*?)\"";
	private static final String VALUE_INFO2_PATTERN = "property *\"valueInfo[.*?]\" *\"(.*?)\"";
	private static final String VALUE_EDITOR_PATTERN = "property *valueEditor *\"(.*?)\"";
	private static final String DYNAMIC_DOMAIN_PATTERN = "property *domainValueProviderName *\"(.*?)\"";
	private static final String VOC_ENTRY_PATTERN = "\\s*(.+?)#.+?\\s*=\\s*(.+)\\s*";
	private static final String JAVA_VERBALIZATION_PATTERN = "\\{this\\}\\.\\w+\\(.*?\\)";

	private static final String EXCEL_PROVIDER = "com.ibm.rules.domainProvider.msexcel2007";

	private static Logger logger = Logger.getLogger(BOMChecker.class.getCanonicalName());

	public BOMChecker(String version, ProjectSelections projectSelections) throws OTAException {
		super(version, projectSelections);
	}

	@Override
	public void run(Report report) throws OTAException {
		logger.info("@ Checking BOM projects from repository");

		Iterator<Item> iter = projectSelections.getSelections();
		try {
			while (iter.hasNext()) {
				Item item = iter.next();
				runOne(report, item);
			}
		} catch (OTAException e) {
			handleElementException(logger, e);
		}
	}

	private void runOne(Report report, Item item) throws OTAException {
		item.setProjectBaseline();
		try {
			report.setBranchContext(item.getBranchName());
			IlrSession session = DCConnection.getSession();
			checkBOM(report, session);
			checkB2X(report, session);
			checkVoc(report, session);
		} finally {
			report.clearBranchContext();
		}
	}

	/**
	 * Check the BOM classes definitions.
	 * 
	 * @param report
	 * @param session
	 * @throws OTAException
	 */
	private void checkBOM(Report report, IlrSession session) throws OTAException {
		try {
			EClass eClass = session.getBrmPackage().getBOM();
			IlrDefaultSearchCriteria criteria = new IlrDefaultSearchCriteria(eClass);
			Set<IlrElementDetails> elements = new HashSet<IlrElementDetails>(session.findElementDetails(criteria));
			for (IlrElementDetails element : elements) {
				IlrBOM bom = (IlrBOM) element;
				bomHolder = bom.getProject();
				if (startVisit(BOMComponent.BOM, bomHolder.getName(), bom.getName())) {
					logger.info("Checking " + bom.getName() + " BOM from project " + bomHolder.getName());
					checkOneBOMElement(report, bom);
				}
			}
		} catch (IlrObjectNotFoundException | IlrRoleRestrictedPermissionException e) {
			throw new OTAException("Error accessing project elements", e);
		}
	}

	/**
	 * Check the B2X definition.
	 * 
	 * @param report
	 * @param session
	 * @throws OTAException
	 */
	private void checkB2X(Report report, IlrSession session) throws OTAException {
		try {
			EClass eClass = session.getBrmPackage().getBOM2XOMMapping();
			IlrDefaultSearchCriteria criteria = new IlrDefaultSearchCriteria(eClass);
			Set<IlrElementDetails> elements = new HashSet<IlrElementDetails>(session.findElementDetails(criteria));
			for (IlrElementDetails element : elements) {
				IlrBOM2XOMMapping b2x = (IlrBOM2XOMMapping) element;
				bomHolder = b2x.getProject();
				if (startVisit(BOMComponent.B2X, bomHolder.getName(), b2x.getName())) {
					logger.info("Checking " + b2x.getName() + " B2X from project " + bomHolder.getName());
					checkOneB2XElement(report, b2x);
				}
			}
		} catch (IlrObjectNotFoundException | IlrRoleRestrictedPermissionException e) {
			throw new OTAException("Error accessing project elements", e);
		}
	}

	/**
	 * Check the vocabulary definition.
	 * 
	 * @param report
	 * @param session
	 * @throws OTAException
	 */
	private void checkVoc(Report report, IlrSession session) throws OTAException {
		try {
			EClass eClass = session.getBrmPackage().getVocabulary();
			IlrDefaultSearchCriteria criteria = new IlrDefaultSearchCriteria(eClass);
			Set<IlrElementDetails> elements = new HashSet<IlrElementDetails>(session.findElementDetails(criteria));
			for (IlrElementDetails element : elements) {
				IlrVocabulary voc = (IlrVocabulary) element;
				bomHolder = voc.getProject();
				if (startVisit(BOMComponent.VOC, bomHolder.getName(), voc.getName())) {
					logger.info("Checking " + voc.getName() + " vocabulary from project " + bomHolder.getName());
					checkOneVOCElement(report, voc);
				}
			}
		} catch (IlrObjectNotFoundException | IlrRoleRestrictedPermissionException e) {
			throw new OTAException("Error accessing project elements", e);
		}
	}

	private void checkOneBOMElement(Report report, IlrBOM bom) {
		checkDomain(report, bom);
		checkValueCustomization(report, bom);
	}

	private void checkOneB2XElement(Report report, IlrBOM2XOMMapping b2x) throws OTAException {
		B2XBrowser view = new B2XBrowser(b2x.getBody());
		for (B2XMember member : view.getMembers()) {
			// Check for deprecated CRE API use.
			if (member.usesDeprecatedAPI()) {
				report.addTextEntry("B2X_USING_CRE_API", bomHolder.getName(), member.getQualifiedName(), b2x);
			}
			// Check for B2X size.
			if (member.getBody().length() > parameters.getInt("maxB2XCharSize")) {
				report.addTextEntry("B2X_CODE_TOO_LONG", bomHolder.getName(), member.getQualifiedName(), b2x);
			}
		}
	}

	private void checkOneVOCElement(Report report, IlrVocabulary voc) throws OTAException {
		Pattern pattern = Pattern.compile(VOC_ENTRY_PATTERN);
		Matcher matcher = pattern.matcher(voc.getBody());
		while (matcher.find()) {
			String vocElement = matcher.group(1);
			String verbalization = matcher.group(2);
			checkVerbalization(report, vocElement, verbalization, voc);
		}
	}

	private void checkDomain(Report report, IlrBOM bom) {
		String body = bom.getBody();
		Pattern pattern = Pattern.compile(DYNAMIC_DOMAIN_PATTERN);
		Matcher matcher = pattern.matcher(body);
		while (matcher.find()) {
			String providerName = matcher.group(1);
			if (!providerName.equals(EXCEL_PROVIDER)) {
				report.addTextEntry("DOMAIN_PROVIDER", bomHolder.getName(), providerName, bom);
			}
		}
	}

	private void checkValueCustomization(Report report, IlrBOM bom) {
		String body = bom.getBody();
		//
		// Checking valueInfo property.
		//
		checkPattern(report, VALUE_INFO1_PATTERN, body, "VALUE_INFO", bom);
		checkPattern(report, VALUE_INFO2_PATTERN, body, "VALUE_INFO", bom);
		//
		// Checking valueEditor property.
		//
		checkPattern(report, VALUE_EDITOR_PATTERN, body, "VALUE_EDITOR", bom);
	}

	private void checkPattern(Report report, String patternString, String body, String tag, IlrBOM context) {
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(body);

		while (matcher.find()) {
			String matched = matcher.group(1);
			report.addTextEntry(tag, bomHolder.getName(), matched, context);
		}
	}

	/**
	 * Checks if the verbalization has a Java format, or has spelling errors.
	 * 
	 * @param report
	 * @param vocElement
	 * @param verbalization
	 * @param context
	 */
	private void checkVerbalization(Report report, String vocElement, String verbalization, IlrVocabulary context)
			throws OTAException {
		//
		// Check for auto-generated, Java-like, verbalizations.
		//
		if (Pattern.matches(JAVA_VERBALIZATION_PATTERN, verbalization)) {
			report.addTextEntry("JAVA_VERBALIZATION", bomHolder.getName(), vocElement, context);
			return;
		}
		//
		// Check for misspelled words.
		//
		List<String> misspelled = VOCSpeller.getSpeller().getMisspelled(verbalization);
		if (misspelled != null) {
			for (String word : misspelled) {
				report.addTextEntry("VERBALIZATION_SPELLING", bomHolder.getName(), word, context);
			}
		}
	}

	/**
	 * Keeps track of the shared BOM elements that have been already processed.
	 * 
	 * @param component
	 * @param projectName
	 * @param entryName
	 * @return
	 */
	private boolean startVisit(BOMComponent component, String projectName, String entryName) {
		String key = component + "." + projectName + "." + entryName;
		if (!visited.contains(key)) {
			visited.add(key);
			return true;
		} else {
			return false;
		}
	}
}
