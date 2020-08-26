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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import ilog.rules.teamserver.brm.IlrRuleProject;
import ilog.rules.teamserver.model.IlrSession;
import ilog.rules.teamserver.model.IlrSessionHelper;

/**
 * Represents the map of all rule projects found in the repository grouped by
 * interdependencies.
 * 
 * @author pberland@us.ibm.com
 *
 */
public class ProjectMap {
	private Map<String, ProjectGroup> map = new HashMap<String, ProjectGroup>();

	public ProjectMap() throws OTAException {
		IlrSession session = DCConnection.getSession();
		List<IlrRuleProject> projects = IlrSessionHelper.getProjects(session);
		for (IlrRuleProject project : projects) {
			addProject(project);
		}
	}

	public Collection<ProjectGroup> getProjectGroups() {
		HashSet<ProjectGroup> groups = new HashSet<ProjectGroup>();
		groups.addAll(map.values());
		return groups;
	}

	private void addProject(IlrRuleProject project) throws OTAException {
		ProjectGroup group = map.get(project.getName());
		if (group == null) {
			group = new ProjectGroup();
			group.add(project);
		}
		for (IlrRuleProject dependent : Helper.getDependencies(project)) {
			ProjectGroup dependentGroup = map.get(dependent.getName());
			if (dependentGroup != null) {
				group.merge(dependentGroup);
			} else {
				group.add(dependent);
			}
		}
		for (String projectName : group.getProjectNames()) {
			map.put(projectName, group);
		}
	}

}
