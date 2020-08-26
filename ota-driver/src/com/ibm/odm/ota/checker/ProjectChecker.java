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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.ibm.odm.ota.DCConnection;
import com.ibm.odm.ota.DecisionRunner;
import com.ibm.odm.ota.OTAException;
import com.ibm.odm.ota.ProjectSelections;
import com.ibm.odm.ota.ProjectSelections.Item;
import com.ibm.odm.ota.Report;

import ilog.rules.teamserver.brm.IlrActionRule;
import ilog.rules.teamserver.brm.IlrRuleProject;
import ilog.rules.teamserver.model.IlrDefaultSearchCriteria;
import ilog.rules.teamserver.model.IlrElementDetails;
import ilog.rules.teamserver.model.IlrObjectNotFoundException;
import ilog.rules.teamserver.model.IlrSession;
import ilog.rules.teamserver.model.permissions.IlrRoleRestrictedPermissionException;

/**
 * Performs checks at the individual rule project level.
 * 
 * @author pberland@us.ibm.com
 *
 */
public class ProjectChecker extends Checker {

	private static final String RULESET_PATH = "/odm/project_validation_operation";

	private static Logger logger = Logger.getLogger(ProjectChecker.class.getCanonicalName());

	public ProjectChecker(String version, ProjectSelections projectSelections) throws OTAException {
		super(version, projectSelections);
	}

	@Override
	public void run(Report report) throws OTAException {
		logger.info("@ Checking individual rule projects from repository");

		Iterator<Item> iter = projectSelections.getSelections();
		try {
			while (iter.hasNext()) {
				runOne(report, iter.next());
			}
		} catch (OTAException e) {
			handleElementException(logger, e);
		}
	}

	public void runOne(Report report, Item item) throws OTAException {
		try {
			IlrRuleProject project = item.getProject();
			item.setProjectBaseline();
			report.setBranchContext(item.getBranchName());
			logger.info("Checking project " + project.getName() + " in branch " + item.getBranchName());

			IlrSession session = DCConnection.getSession();
			IlrDefaultSearchCriteria criteria = new IlrDefaultSearchCriteria(
					session.getBrmPackage().getProjectElement());
			List<IlrElementDetails> elements = session.findElementDetails(criteria);

			runRulesetChecks(report, project, elements);
			runJavaChecks(report, project, elements);
			// runExperimental(project, elements);

		} catch (IlrRoleRestrictedPermissionException | IlrObjectNotFoundException e) {
			throw new OTAException("Error accessing project elements", e);
		} finally {
			report.clearBranchContext();
		}
	}

	/**
	 * Runs the project validation ruleset.
	 * 
	 * @param report
	 * @param project
	 * @param elements
	 * @throws OTAException
	 */
	private void runRulesetChecks(Report report, IlrRuleProject project, List<IlrElementDetails> elements)
			throws OTAException {
		DecisionRunner runner = new DecisionRunner(version, RULESET_PATH);
		Map<String, Object> inputParameters = new HashMap<String, Object>();
		inputParameters.put("project", project);
		inputParameters.put("elements", elements);
		inputParameters.put("parameters", parameters);
		inputParameters.put("report", report);
		runner.run(inputParameters);

	}

	/**
	 * Runs additional project validations.
	 * 
	 * @param report
	 * @param project
	 * @param elements
	 * @throws OTAException
	 */
	private void runJavaChecks(Report report, IlrRuleProject project, List<IlrElementDetails> elements)
			throws OTAException {
		// TODO: Add validation backlog.
	}

	/**
	 * Experimental code on element details from the repository is done here.
	 * Invocation of this method should be removed from final code.
	 * 
	 * @param project
	 * @param elements
	 * @throws OTAException
	 */
	@SuppressWarnings("unused")
	private void runExperimental(IlrRuleProject project, List<IlrElementDetails> elements) throws OTAException {
		for (IlrElementDetails details : elements) {
			if (details.getType().equals("brm.ActionRule")) {
				IlrActionRule rule = ((IlrActionRule) details);
			}
		}
	}

}
