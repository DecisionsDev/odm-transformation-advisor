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

import java.io.File;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import ilog.rules.res.model.IlrFormatException;
import ilog.rules.res.model.IlrPath;
import ilog.rules.res.session.IlrJ2SESessionFactory;
import ilog.rules.res.session.IlrManagementSession;
import ilog.rules.res.session.IlrSessionException;
import ilog.rules.res.session.IlrSessionFactory;
import ilog.rules.res.session.IlrSessionRequest;
import ilog.rules.res.session.IlrSessionResponse;
import ilog.rules.res.session.IlrStatelessSession;
import ilog.rules.res.session.config.IlrFilePersistenceConfig;
import ilog.rules.res.session.config.IlrPersistenceConfig;
import ilog.rules.res.session.config.IlrPersistenceType;
import ilog.rules.res.session.config.IlrSessionFactoryConfig;
import ilog.rules.res.session.config.IlrXUConfig;

/**
 * Manages a very generic JavaSE execution of rulesets
 * 
 * @author pberland@us.ibm.com
 *
 */
public class DecisionRunner {
	private IlrSessionFactory factory;
	private IlrPath rulesetPath;

	private static final String REPOSITORY = "repository";

	private static Logger logger = Logger.getLogger(DecisionRunner.class.getName());

	public DecisionRunner(String version, String rulesetPathString) throws OTAException {
		configureFactory(version);
		loadRuleset(rulesetPathString);
	}

	public static String getRepositoryPath(String version) {
		return ClassLoader.getSystemResource(REPOSITORY).getPath() + File.separator + version;
	}

	public static String getXOMPath() {
		return ClassLoader.getSystemResource(REPOSITORY).getPath() + File.separator + ".." + File.separator + "xom";
	}

	public Map<String, Object> run(Map<String, Object> inputParameters) throws OTAException {
		try {
			IlrStatelessSession session = factory.createStatelessSession();
			IlrSessionRequest sessionRequest = factory.createRequest();
			sessionRequest.setRulesetPath(rulesetPath);

			sessionRequest.setTraceEnabled(true);
			sessionRequest.setInputParameters(inputParameters);

			IlrSessionResponse sessionResponse = null;

			long t0 = System.currentTimeMillis();
			sessionResponse = session.execute(sessionRequest);
			long t1 = System.currentTimeMillis();

			logger.fine("Exec time: " + (t1 - t0));

			return sessionResponse.getOutputParameters();
		} catch (IlrSessionException e) {
			throw new OTAException("Error running ruleset " + rulesetPath, e);
		}
	}

	public void configureFactory(String version) throws OTAException {

		IlrSessionFactoryConfig sessionConfig = IlrJ2SESessionFactory.createDefaultConfig();
		IlrXUConfig xuConfig = sessionConfig.getXUConfig();
		xuConfig.setLogLevel(Level.WARNING);

		IlrPersistenceConfig rappPersistence = xuConfig.getPersistenceConfig();
		rappPersistence.setPersistenceType(IlrPersistenceType.FILE);
		IlrFilePersistenceConfig rappConfig = rappPersistence.getFilePersistenceConfig();

		File ruleappPath = new File(getRepositoryPath(version));
		if (!ruleappPath.exists()) {
			throw new OTAException("Invalid OTA version parameter: " + version);
		}
		rappConfig.setDirectory(ruleappPath);

		IlrPersistenceConfig xomPersistence = xuConfig.getManagedXOMPersistenceConfig();
		xomPersistence.setPersistenceType(IlrPersistenceType.FILE);
		IlrFilePersistenceConfig xomConfig = xomPersistence.getFilePersistenceConfig();
		xomConfig.setDirectory(new File(getXOMPath()));

		factory = new IlrJ2SESessionFactory(sessionConfig);
	}

	public void loadRuleset(String rulesetPathString) throws OTAException {
		try {
			rulesetPath = IlrPath.parsePath(rulesetPathString);
			logger.fine("Using path : " + rulesetPath);

			IlrManagementSession managementSession = factory.createManagementSession();
			managementSession.loadUptodateRuleset(rulesetPath);
		} catch (IlrFormatException | IlrSessionException e) {
			throw new OTAException("Error loading ruleset", e);
		}
	}
}
