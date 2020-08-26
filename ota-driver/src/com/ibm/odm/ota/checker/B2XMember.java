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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class B2XMember {
	private String className;
	private String name;
	private String type;
	private String body;

	public static final String GETTER = "getter";
	public static final String SETTER = "setter";
	public static final String METHOD = "method";
	public static final String FIELD = "field";
	public static final String VARIABLE = "variable";

	public static final String CONTEXT = "ilog.rules.engine.IlrContext";
	public static final String INSTANCE = "ilog.rules.engine.IlrRuleInstance";

	private static Hashtable<String, Hashtable<String, List<String>>> deprecated;

	private static Logger logger = Logger.getLogger(B2XMember.class.getCanonicalName());

	static {
		try {
			deprecated = new Hashtable<String, Hashtable<String, List<String>>>();
			addUsageFrom(CONTEXT, new String[] { "?context", "context" }, true);
			addUsageFrom(INSTANCE, new String[] { "?instance", "instance" }, false);
		} catch (ClassNotFoundException e) {
			logger.warning("Could not initialize the deprecated table");
			deprecated = null;
		}
	}

	/**
	 * Adds public methods and public fields from the given class name to the
	 * deprecated table, as well as the given variable names.
	 * 
	 * @param className
	 */
	private static void addUsageFrom(String className, String[] varNames, boolean includeMembers)
			throws ClassNotFoundException {
		Hashtable<String, List<String>> usage = new Hashtable<String, List<String>>();
		List<String> variables = Arrays.asList(varNames);
		usage.put(VARIABLE, variables);

		if (includeMembers) {
			Class<?> clazz = Class.forName(className);
			Class<?> objectClazz = Class.forName("java.lang.Object");
			//
			// Add public methods (except the ones from the Object class).
			//
			List<Method> objectMethods = Arrays.asList(objectClazz.getMethods());
			List<String> methodNames = new ArrayList<String>();
			for (Method method : clazz.getMethods()) {
				if (Modifier.isPublic(method.getModifiers()) && !objectMethods.contains(method)) {
					methodNames.add(method.getName());
				}
			}
			usage.put(METHOD, methodNames);
			//
			// Add public fields (except the ones from the Object class).
			//
			List<Field> objectFields = Arrays.asList(objectClazz.getFields());
			List<String> fieldNames = new ArrayList<String>();
			for (Field field : clazz.getFields()) {
				if (Modifier.isPublic(field.getModifiers()) && !objectFields.contains(field)) {
					methodNames.add(field.getName());
				}
			}
			usage.put(FIELD, fieldNames);
		}

		deprecated.put(className, usage);
	}

	public B2XMember(String className, String name, String type, String body) {
		this.className = className;
		this.name = name;
		this.type = type;
		this.body = body;
	}

	public String getClassName() {
		return className;
	}

	public String getName() {
		return name;
	}

	public String getBody() {
		return body;
	}

	public String getType() {
		return type;
	}

	public String getQualifiedName() {
		return className + "." + name;
	}

	public boolean usesDeprecatedAPI() {
		return uses(INSTANCE) || uses(CONTEXT);
	}

	/**
	 * Checks in sequence whether any of the variables, the methods or the fields of
	 * the deprecated class are used in the B2X member body.
	 * 
	 * @param className
	 * @return
	 */
	private boolean uses(String className) {
		Hashtable<String, List<String>> usage = deprecated.get(className);
		for (String type : usage.keySet()) {
			if (uses(usage, type)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check for reference to one of the deprecated variable, method, or field in
	 * the body of the B2X member.
	 * 
	 * @param usage
	 * @param type
	 * @return
	 */
	private boolean uses(Hashtable<String, List<String>> usage, String type) {

		List<String> candidates = usage.get(type);
		for (String candidate : candidates) {
			String pattern = null;
			switch (type) {
			case VARIABLE:
				pattern = "[^A-Za-z0-9_]" + candidate + "[^A-Za-z0-9_]";
				break;
			case METHOD:
				pattern = "[^A-Za-z0-9_]" + candidate + "\\s*\\(";
				break;
			case FIELD:
				pattern = "[^A-Za-z0-9_]" + candidate + "[^A-Za-z0-9_]";
				break;
			}
			if (Pattern.compile(pattern).matcher(body).find()) {
				return true;
			}
		}
		return false;
	}
}
