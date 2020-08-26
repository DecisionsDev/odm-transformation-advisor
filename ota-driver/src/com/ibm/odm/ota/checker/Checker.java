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

import java.util.logging.Logger;

import com.ibm.odm.ota.Findings;
import com.ibm.odm.ota.OTAException;
import com.ibm.odm.ota.Parameters;
import com.ibm.odm.ota.ProjectSelections;
import com.ibm.odm.ota.Report;

/**
 * Provides checker interface. TODO: Register the checkers we want to activate
 * in a property file?
 * 
 * @author pberland@us.ibm.com
 *
 */
public abstract class Checker {

	private boolean oneExceptionFlagged = false;
	protected String version;
	protected ProjectSelections projectSelections;
	protected Parameters parameters;

	protected Checker(String version, ProjectSelections projectSelections) throws OTAException {
		this.version = version;
		this.projectSelections = projectSelections;
		this.parameters = Findings.getParameters();
	}

	abstract public void run(Report report) throws OTAException;

	/**
	 * Avoid repetition of the stack trace for the following elements.
	 * 
	 * @param logger
	 * @param e
	 */
	protected void handleElementException(Logger logger, OTAException e) {
		if (!oneExceptionFlagged) {
			logger.severe(e.getStackTraceString());
			oneExceptionFlagged = true;
		} else {
			logger.severe(e.getMessage());
		}
	}
}
