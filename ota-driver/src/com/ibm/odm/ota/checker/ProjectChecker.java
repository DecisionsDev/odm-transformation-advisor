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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.ibm.odm.ota.DCConnection;
import com.ibm.odm.ota.DecisionRunner;
import com.ibm.odm.ota.OTAException;
import com.ibm.odm.ota.Report;

/**
 * Performs checks at the individual rule project level.
 * 
 * @author pberland@us.ibm.com
 *
 */
public class ProjectChecker extends Checker {

	private static final String RULESET_PATH = "/odm/project_validation_operation";

	private static Logger logger = Logger.getLogger(ProjectChecker.class
			.getCanonicalName());

	public ProjectChecker(String version, List<String> targetProjects) throws OTAException {
		super(version, targetProjects);
	}

	@Override
	public void run(Report report) throws OTAException {
		logger.info("@ Checking individual rule projects from repository");
		IlrSession session = DCConnection.getSession();
		List<IlrRuleProject> projects = IlrSessionHelper.getProjects(session);
		for (IlrRuleProject project : projects) {
			try {
				if (isTargetProject(project)) {
					runOne(report, project);
				}
			} catch (OTAException e) {
				handleElementException(logger, e);
			}
		}
	}

	public void runOne(Report report, IlrRuleProject project)
			throws OTAException {
		try {
			logger.info("Checking project " + project.getName());
			IlrSession session = DCConnection.getSession();
			IlrBaseline currentBaseline = IlrSessionHelper.getCurrentBaseline(
					session, project);
			session.setWorkingBaseline(currentBaseline);

			IlrDefaultSearchCriteria criteria = new IlrDefaultSearchCriteria(
					session.getBrmPackage().getProjectElement());
			List<IlrElementDetails> elements = session
					.findElementDetails(criteria);

			runRulesetChecks(report, project, elements);
			runJavaChecks(report, project, elements);
			runDebugStuff(project, elements);
		} catch (IlrObjectNotFoundException | IlrPermissionException e) {
			throw new OTAException("Error accessing project elements", e);
		}
	}

	private void runRulesetChecks(Report report, IlrRuleProject project,
			List<IlrElementDetails> elements) throws OTAException {
		DecisionRunner runner = new DecisionRunner(version, RULESET_PATH);
		Map<String, Object> inputParameters = new HashMap<String, Object>();
		inputParameters.put("project", project);
		inputParameters.put("elements", elements);
		inputParameters.put("parameters", parameters);
		inputParameters.put("report", report);
		runner.run(inputParameters);

	}

	private void runJavaChecks(Report report, IlrRuleProject project,
			List<IlrElementDetails> elements) throws OTAException {
		// TODO
	}

	/**
	 * Test & debug code on element details from the repository is done here.
	 * Invocation of this method should be removed from final code.
	 * 
	 * @param project
	 * @param elements
	 * @throws OTAException
	 */
	private void runDebugStuff(IlrRuleProject project,
			List<IlrElementDetails> elements) throws OTAException {
		for (IlrElementDetails details : elements) {
			if (details.getType().equals("brm.ActionRule")) {
				/*
				 * IlrActionRule rule = ((IlrActionRule) details); if (
				 * rule.getName().equals("vague name")) { IlrBrmPackage model =
				 * DCConnection.getSession().getBrmPackage();
				 * rule.setRawValue(model.getProjectElement_Documentation(),
				 * "COMMENT"); DCConnection.getSession().commit(rule);
				 * System.out.println("VAGUE NAME COMMENTED"); }
				 */
				/*
				 * String body = rule.getDefinition().getBody();
				 * System.out.println("--------------");
				 * System.out.println(body); ActionRuleInfo ari = new
				 * ActionRuleInfo(body); System.out.println(ari.getInfo());
				 */

				/*
				 * IlrRulePackage pkg = ((IlrActionRule)
				 * details).getRulePackage();
				 * System.out.println(pkg.getPropertyValue(arg0)); if (pkg !=
				 * null)
				 * 
				 * { if (pkg.getChildren().size() > 0)
				 * System.out.println(pkg.getName()); }
				 */
			}
		}

	}

}
