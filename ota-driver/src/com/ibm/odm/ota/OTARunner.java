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

import java.util.logging.Logger;

import com.ibm.odm.ota.checker.BOMChecker;
import com.ibm.odm.ota.checker.ProjectChecker;
import com.ibm.odm.ota.checker.ProjectGroupChecker;
import com.ibm.odm.ota.checker.RepositoryChecker;

/**
 * Defines the main entry point.
 * 
 * @author pberland@us.ibm.com
 *
 */
public class OTARunner {

	private String username;
	private String password;
	private String url;
	private String datasource;
	private String projects;
	private String version;

	private String report;

	private static final String DEFAULT_VERSION = "v810";
	private static final String DEFAULT_USERNAME = "rtsAdmin";
	private static final String DEFAULT_PASSWORD = "rtsAdmin";
	private static final String DEFAULT_URL = "http://localhost:9090/teamserver";
	private static final String DEFAULT_REPORT = "ota-report.html";

	private static Logger logger = Logger.getLogger(OTARunner.class.getCanonicalName());

	private OTARunner(String[] args) throws OTAException {
		getParams(args);
	}

	/**
	 * Gets the parameters from the execution arguments. The parameter values are
	 * parsed and validated just in time, in the place they are used.
	 * 
	 * @param args
	 * @throws OTAException
	 */
	private void getParams(String[] args) throws OTAException {
		if (args.length == 0) {
			url = DEFAULT_URL;
			datasource = null;
			projects = null;
			username = DEFAULT_USERNAME;
			password = DEFAULT_PASSWORD;
			report = DEFAULT_REPORT;
			version = DEFAULT_VERSION;
		} else {
			url = getArg("url", args);
			datasource = getArg("datasource", args);
			username = getArg("username", args);
			password = getArg("password", args);
			report = getArg("report", args);
			version = getArg("version", args);
			projects = getArg("projects", args);
		}
	}

	private String getArg(String key, String[] args) throws OTAException {
		for (String arg : args) {
			String[] kvp = arg.split("=");
			if (kvp[0].equals(key)) {
				return (kvp.length == 1 || kvp[1].trim().isEmpty()) ? null : kvp[1];
			}
		}
		throw new OTAException("Argument " + key + " not found", null);
	}

	private void run() throws OTAException {
		DCConnection.startSession(url, username, password, datasource);
		Report runReport = new Report(url, datasource, username);

		ProjectSelections projectSelections = new ProjectSelections(projects);

		logger.info("Starting repository analysis for " + url + (datasource != null ? "/" + datasource : ""));
		(new ProjectChecker(version, projectSelections)).run(runReport);
		(new ProjectGroupChecker(version, projectSelections)).run(runReport);
		(new RepositoryChecker(version, projectSelections)).run(runReport);
		(new BOMChecker(version, projectSelections)).run(runReport);

		ReportFormatter formatter = new ReportFormatter();
		formatter.createHTML(runReport, report);
		logger.info("Analysis completed, results available in " + report);

		DCConnection.endSession();
	}

	public static void main(String[] args) {
		try {
			OTARunner runner = new OTARunner(args);
			runner.run();
		} catch (OTAException e) {
			logger.severe(e.getStackTraceString());
		}
	}
}
