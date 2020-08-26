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

import ilog.rules.teamserver.client.IlrRemoteSessionFactory;
import ilog.rules.teamserver.model.IlrConnectException;
import ilog.rules.teamserver.model.IlrSession;
import ilog.rules.teamserver.model.IlrSessionFactory;

/**
 * Manages connection with the DC repository.
 * 
 * @author pberland@us.ibm.com
 *
 */
public class DCConnection {

	private static IlrSession session = null;

	private static Logger logger = Logger.getLogger(DCConnection.class.getCanonicalName());

	private DCConnection() {
	}

	/**
	 * Returns the session associated with DC reference.
	 * 
	 * @return
	 */
	public static IlrSession getSession() {
		return session;
	}

	public static void startSession(String serverURL, String login, String password, String datasource)
			throws OTAException {
		try {
			IlrSessionFactory factory = new IlrRemoteSessionFactory();
			factory.connect(login, password, serverURL, datasource);
			session = factory.getSession();
			session.beginUsage();
		} catch (IlrConnectException e) {
			throw new OTAException("Error connecting to ODM repository with the given URL and credentials", e);
		}
	}

	public static void endSession() {
		if (session != null) {
			session.endUsage();
			session.close();
			logger.info("Ending use of DC session");
		} else {
			logger.info("No current DC session");
		}
	}
}
