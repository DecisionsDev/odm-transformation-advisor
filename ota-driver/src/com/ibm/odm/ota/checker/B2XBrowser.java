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

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ibm.odm.ota.OTAException;

public class B2XBrowser {

	private List<B2XMember> members;

	public B2XBrowser(String input) throws OTAException {
		members = new ArrayList<B2XMember>();
		parseB2X(input);
	}

	public List<B2XMember> getMembers() {
		return members;
	}

	private void parseB2X(String input) throws OTAException {
		try {

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(input)));
			doc.getDocumentElement().normalize();

			NodeList classList = getChildren(doc.getDocumentElement(), "class");
			for (int classIndex = 0; classIndex < classList.getLength(); classIndex++) {
				Node clazz = classList.item(classIndex);
				String className = getValue(clazz, "businessName");
				//
				// Gathering getters and setters for the class.
				//
				NodeList attrList = getChildren(clazz, "attribute");
				for (int attrIndex = 0; attrIndex < attrList.getLength(); attrIndex++) {
					Node attr = attrList.item(attrIndex);
					String attrName = getValue(attr, "name");
					String getter = getValue(attr, B2XMember.GETTER);
					if (getter != null) {
						B2XMember member = new B2XMember(className, attrName, B2XMember.GETTER, getter);
						members.add(member);
					}
					String setter = getValue(attr, B2XMember.SETTER);
					if (setter != null) {
						B2XMember member = new B2XMember(className, attrName, B2XMember.SETTER, setter);
						members.add(member);
					}
				}
				//
				// Gathering methods for the class.
				//
				NodeList methodList = getChildren(clazz, B2XMember.METHOD);
				for (int methodIndex = 0; methodIndex < methodList.getLength(); methodIndex++) {
					Node method = methodList.item(methodIndex);
					String methodName = getValue(method, "name");
					String methodBody = getValue(method, "body");
					B2XMember member = new B2XMember(className, methodName, B2XMember.METHOD, methodBody);
					members.add(member);
				}
			}
		} catch (IOException | ParserConfigurationException | SAXException e) {
			throw new OTAException("Error parsing B2X", e);
		}
	}

	private NodeList getChildren(Node node, String name) {
		return ((Element) node).getElementsByTagName(name);
	}

	private String getValue(Node node, String name) {
		NodeList children = ((Element) node).getElementsByTagName(name);
		return (children.getLength() == 1) ? children.item(0).getTextContent() : null;
	}
}
