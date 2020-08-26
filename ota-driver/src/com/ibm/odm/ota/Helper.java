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
import java.util.List;

import ilog.rules.teamserver.brm.IlrBaseline;
import ilog.rules.teamserver.brm.IlrDependency;
import ilog.rules.teamserver.brm.IlrRuleProject;
import ilog.rules.teamserver.model.IlrObjectNotFoundException;
import ilog.rules.teamserver.model.IlrSession;
import ilog.rules.teamserver.model.IlrSessionHelper;
import ilog.rules.teamserver.model.permissions.IlrPermissionException;

/**
 * Provides helper functions.
 * 
 * @author pberland@us.ibm.com
 *
 */
public class Helper {

	public static IlrRuleProject getProjectNamed(String projectName) throws OTAException {
		try {
			return IlrSessionHelper.getProjectNamed(DCConnection.getSession(), projectName);
		} catch (IlrObjectNotFoundException e) {
			throw new OTAException("Error getting project " + projectName, e);
		}
	}

	public static List<IlrRuleProject> getDependencies(IlrRuleProject project) throws OTAException {
		try {
			List<IlrRuleProject> projects = new ArrayList<IlrRuleProject>();
			IlrSession session = DCConnection.getSession();
			IlrBaseline currentBaseline = IlrSessionHelper.getCurrentBaseline(session, project);
			session.setWorkingBaseline(currentBaseline);
			@SuppressWarnings("unchecked")
			List<IlrDependency> deps = currentBaseline.getProjectInfo().getDependencies();
			for (IlrDependency dep : deps) {
				IlrRuleProject dependent = IlrSessionHelper.getProjectNamed(session, dep.getProjectName());
				projects.add(dependent);
			}
			return projects;
		} catch (IlrObjectNotFoundException | IlrPermissionException e) {
			throw new OTAException("Error getting project dependencies", e);
		}
	}

}
