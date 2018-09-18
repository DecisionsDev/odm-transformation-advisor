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

import ilog.rules.teamserver.brm.IlrBaseline;
import ilog.rules.teamserver.brm.IlrRuleProject;
import ilog.rules.teamserver.model.IlrDefaultSearchCriteria;
import ilog.rules.teamserver.model.IlrElementDetails;
import ilog.rules.teamserver.model.IlrObjectNotFoundException;
import ilog.rules.teamserver.model.IlrSession;
import ilog.rules.teamserver.model.IlrSessionHelper;
import ilog.rules.teamserver.model.permissions.IlrPermissionException;
import ilog.rules.teamserver.model.permissions.IlrSecurityProfileData;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EStructuralFeature;

import com.ibm.odm.ota.DCConnection;
import com.ibm.odm.ota.OTAException;
import com.ibm.odm.ota.Report;

/**
 * Performs checks that are not easily implemented with rules.
 * 
 * @author pberland@us.ibm.com
 *
 */
public class RepositoryChecker extends Checker {

	private static Logger logger = Logger.getLogger(RepositoryChecker.class
			.getCanonicalName());

	public RepositoryChecker(String version, List<String> targetProjects) throws OTAException {
		super(version, targetProjects);
	}

	@Override
	public void run(Report report) throws OTAException {
		logger.info("@ Checking repository characteristics");
		//
		// Fine-grained permission may get deprecated in vNext.
		// checkPermissions(report);
		//
		checkCustomProperties(report);
	}

	/**
	 * Check the use of fine grained permissions.
	 * 
	 * @param report
	 */
	@SuppressWarnings("unused")
	private void checkPermissions(Report report) {
		IlrSession session = DCConnection.getSession();
		for (String group : session.getAvailableGroups()) {
			IlrSecurityProfileData sec = session.getSecurityProfileData(group);
			if (sec.size() > 0) {
				report.addTextEntry("PERMISSIONS", group,
						Integer.toString(sec.size()), null);
			}
		}
	}

	/**
	 * Check the use of custom properties for the rules.
	 * 
	 * @param report
	 * @throws OTAException
	 */
	private void checkCustomProperties(Report report) throws OTAException {
		List<String> predefined = Arrays.asList(new String[] { "effectiveDate",
				"expirationDate", "status", "overriddenRules", "priority" });
		IlrElementDetails element = getOneRule();
		if (element != null) {
			EList<EStructuralFeature> features = element.eClass()
					.getEAllStructuralFeatures();
			for (EStructuralFeature feature : features) {
				if (feature.getEContainingClass().getName().equals("Rule")
						&& !predefined.contains(feature.getName())) {
					report.addTextEntry("CUSTOM_PROPERTY", "", feature.getName(),
							null);
				}
			}
		}
	}

	private IlrElementDetails getOneRule() throws OTAException {
		try {
			IlrSession session = DCConnection.getSession();
			List<IlrRuleProject> projects = IlrSessionHelper
					.getProjects(session);
			for (IlrRuleProject project : projects) {
				IlrBaseline currentBaseline = IlrSessionHelper
						.getCurrentBaseline(session, project);
				session.setWorkingBaseline(currentBaseline);

				IlrDefaultSearchCriteria criteria = new IlrDefaultSearchCriteria(
						session.getBrmPackage().getBusinessRule());
				List<IlrElementDetails> elements = session
						.findElementDetails(criteria);
				if (elements.size() > 0) {
					return elements.get(0);
				}
			}
			return null;
		} catch (IlrObjectNotFoundException | IlrPermissionException e) {
			throw new OTAException("Error accessing project elements", e);
		}
	}

}
