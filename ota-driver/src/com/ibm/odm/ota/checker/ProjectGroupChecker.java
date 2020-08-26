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
import java.util.Map;
import java.util.logging.Logger;

import com.ibm.odm.ota.DecisionRunner;
import com.ibm.odm.ota.Findings;
import com.ibm.odm.ota.OTAException;
import com.ibm.odm.ota.ProjectGroup;
import com.ibm.odm.ota.ProjectMap;
import com.ibm.odm.ota.ProjectSelections;
import com.ibm.odm.ota.Report;

import ilog.rules.teamserver.brm.IlrRuleProject;

/**
 * Performs checks at the level of groups of dependent projects (for classic
 * projects) or projects part of a decision service.
 * 
 * @author pberland@us.ibm.com
 *
 */
public class ProjectGroupChecker extends Checker {
	public static final String RULESET_PATH = "/odm/project_group_validation_operation";

	private static Logger logger = Logger.getLogger(ProjectGroupChecker.class.getCanonicalName());

	public ProjectGroupChecker(String version, ProjectSelections projectSelections) throws OTAException {
		super(version, projectSelections);
	}

	@Override
	public void run(Report report) throws OTAException {
		logger.info("@ Checking project groups or decision services from repository");
		ProjectMap map = new ProjectMap();
		for (ProjectGroup group : map.getProjectGroups()) {
			try {
				if (isSelected(group)) {
					runOne(report, group);
				}
			} catch (OTAException e) {
				handleElementException(logger, e);
			}
		}
	}

	/**
	 * A project group is selected if any of the project in the group is selected.
	 * 
	 * @param group
	 * @return
	 */
	public boolean isSelected(ProjectGroup group) {
		for (String project : group.getProjectNames()) {
			if (projectSelections.isSelected(project)) {
				return true;
			}
		}
		return false;
	}

	public void runOne(Report report, ProjectGroup group) throws OTAException {
		DecisionRunner runner = new DecisionRunner(version, RULESET_PATH);

		Map<String, Object> inputParameters = new HashMap<String, Object>();
		IlrRuleProject keyProject = group.getKeyProject();
		logger.info("Checking project group or decision service " + keyProject.getName());
		inputParameters.put("project", keyProject);
		inputParameters.put("report", report);
		inputParameters.put("parameters", Findings.getParameters());
		runner.run(inputParameters);
	}
}
