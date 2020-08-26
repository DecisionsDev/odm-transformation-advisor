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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ilog.rules.teamserver.brm.IlrRuleProject;

/**
 * Represents a group of interdependent rule projects (classic projects or
 * decision services).
 * 
 * @author pberland@us.ibm.com
 *
 */
public class ProjectGroup {

	private String decisionServiceName = null;
	private Set<String> projectNames = new HashSet<String>();

	public Set<String> getProjectNames() {
		return projectNames;
	}

	public String getDecisionServiceName() {
		return decisionServiceName;
	}

	public void add(IlrRuleProject project) {
		String projectName = project.getName();
		projectNames.add(projectName);
		if (project.isDecisionService()) {
			decisionServiceName = projectName;
		}
	}

	public void merge(ProjectGroup merged) {
		projectNames.addAll(merged.projectNames);
		if (merged.decisionServiceName != null) {
			decisionServiceName = merged.decisionServiceName;
		}
	}

	public List<IlrRuleProject> getProjects() throws OTAException {
		List<IlrRuleProject> projects = new ArrayList<IlrRuleProject>();
		for (String projectName : projectNames) {
			projects.add(Helper.getProjectNamed(projectName));
		}
		return projects;
	}

	public IlrRuleProject getDecisionService() throws OTAException {
		return (decisionServiceName == null) ? null : Helper.getProjectNamed(decisionServiceName);
	}

	public IlrRuleProject getKeyProject() throws OTAException {
		if (decisionServiceName != null) {
			return getDecisionService();
		} else {
			return Helper.getProjectNamed(projectNames.iterator().next());
		}
	}
}
