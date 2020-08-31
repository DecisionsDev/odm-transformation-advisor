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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import ilog.rules.teamserver.brm.IlrBaseline;
import ilog.rules.teamserver.brm.IlrRuleProject;
import ilog.rules.teamserver.model.IlrObjectNotFoundException;
import ilog.rules.teamserver.model.IlrSession;
import ilog.rules.teamserver.model.IlrSessionHelper;
import ilog.rules.teamserver.model.permissions.IlrPermissionException;

/**
 * Manages the list of project/branches selected for analysis.
 * 
 * @author pberland@us.ibm.com
 *
 */
public class ProjectSelections {
	private List<Item> selections = null;

	private static Logger logger = Logger.getLogger(ProjectSelections.class.getCanonicalName());

	public ProjectSelections(String projectSelectionsString) throws OTAException {
		this.selections = getSelectedItems(projectSelectionsString);
	}

	/**
	 * Returns an iterator on all selected project/branch items.
	 * @return
	 */
	public Iterator<Item> getSelections() {
		return selections.iterator();
	}
	
	/**
	 * Return true if the given project name is selected for analysis.
	 * 
	 * @param projectName
	 * @return
	 */
	public boolean isSelected(String projectName) {
		for (Item item : selections) {
			if (item.getProject().getName().equals(projectName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the list of project/branch items from the given parameter string. The
	 * format of the string is the following:
	 * 
	 * <pre>
	 * <project-spec> ::= <project-name> | <project-name> "/" <branch-name>
	 * <ota-projects> ::= <project-spec> | <project-spec> ":" <ota-projects>
	 * </pre>
	 * 
	 * If the project selection string is null, all the projects from the repository
	 * with the current branch are returned.
	 * 
	 * @param projectSelectionsString
	 * @return ArrayList<Item>
	 * @throws OTAException
	 */
	protected List<Item> getSelectedItems(String projectSelectionsString) throws OTAException {
		final String projectSeparator = ":";
		final String branchSeparator = "/";

		List<Item> selections = new ArrayList<Item>();
		IlrSession session = DCConnection.getSession();

		if (projectSelectionsString == null) {
			List<IlrRuleProject> projects = IlrSessionHelper.getProjects(session);
			for (IlrRuleProject project : projects) {
				selections.add(new Item(project, null));
			}
		} else {
			logger.info("@ Processing project list");
			projectSelectionsString.trim();

			String[] projectSelections = projectSelectionsString.trim().split(" *" + projectSeparator + " *");

			String projectName;
			String branchName;
			for (String projectSelection : projectSelections) {
				if (projectSelection.isEmpty()) {
					throw new OTAException("Invalid (empty) project selection: " + projectSelectionsString);
				}
				if (projectSelection.contains(branchSeparator)) {
					String[] pair = projectSelection.trim().split(" *" + branchSeparator + " *");
					if (pair.length != 2 || pair[0].isEmpty() || pair[1].isEmpty()) {
						throw new OTAException("Invalid project/branch selection: '" + projectSelection + "'");
					}
					projectName = pair[0];
					branchName = pair[1];
				} else {
					projectName = projectSelection;
					branchName = null;
				}
				try {
					IlrRuleProject project = IlrSessionHelper.getProjectNamed(session, projectName);
					if ( project == null ) {
						throw new OTAException("Invalid rule project name: " + projectName);
					}
					else {
					selections.add(new Item(project, branchName));
					}
				} catch (IlrObjectNotFoundException e) {
					throw new OTAException("Invalid rule project name: " + projectName);
				}
			}
		}
		return selections;
	}

	/**
	 * Represents a project to be analyzed in a given branch, or in the current
	 * branch when the branch is null.
	 * 
	 * @author pberland@us.ibm.com
	 *
	 */
	public class Item {
		private IlrRuleProject project;
		private String branch;

		public Item(IlrRuleProject project, String branch) {
			this.project = project;
			this.branch = branch;
		}

		public IlrRuleProject getProject() {
			return project;
		}

		public String getBranch() {
			return branch;
		}

		public String getBranchName() {
			return (branch == null) ? "[current]" : branch;
		}

		/**
		 * Sets the target baseline for the given project as given by the targetProjects
		 * table.
		 * 
		 * @param session
		 * @param project
		 * @throws OTAException
		 */
		public void setProjectBaseline() throws OTAException {
			try {
				IlrBaseline targetBaseline;
				IlrSession session = DCConnection.getSession();
				if (branch != null) {
					targetBaseline = IlrSessionHelper.getBaselineNamed(session, project, branch);
				} else {
					targetBaseline = IlrSessionHelper.getCurrentBaseline(session, project);
				}
				session.setWorkingBaseline(targetBaseline);
			} catch (IlrObjectNotFoundException | IlrPermissionException e) {
				throw new OTAException("Error accessing baseline for project " + project.getName(), e);
			}
		}

	}

}
