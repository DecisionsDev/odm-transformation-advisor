package com.ibm.odm.ota;

import java.util.HashMap;
import java.util.Map;
import ilog.rules.bom.annotations.NotBusiness;

/**
 * Findings parameters, used in rules or Java validations.
 * 
 * @author pberland@us.ibm.com
 *
 */
public class Parameters {
	private Map<String, String> params = new HashMap<String, String>();

	@NotBusiness
	public void put(String key, String value) {
		params.put(key, value);
	}

	public int getInt(String key) {
		return Integer.parseInt(params.get(key));
	}

	public String getString(String key) {
		return params.get(key);
	}
}
