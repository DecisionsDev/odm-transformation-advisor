package com.ibm.odm.ota;

import ilog.rules.teamserver.brm.IlrActionRule;
import ilog.rules.teamserver.model.IlrObjectNotFoundException;
import ilog.rules.bom.annotations.NotBusiness;

/**
 * This one is a bit of a makeshift, to try to get some information out of the
 * textual form of the action rule, without resorting to complex parsing. Any
 * suggestion on making this simpler and more accurate is welcome!
 * 
 * @author pberland@us.ibm.com
 *
 */
public class ActionRuleInfo {

	private String body;
	private String found;

	private int definitionsCount = 0;
	private int ifCount = 0;
	private int thenCount = 0;
	private int elseCount = 0;

	private int countOfConstantsInDefinitions = 0;
	private int countOfConstantsInConditions = 0;

	private static final String VAR = "VARIABLE";
	private static final String CST = "CONSTANT";

	private static final String DEFINITIONS = "definitions";
	private static final String IF = "if";
	private static final String THEN = "then";
	private static final String ELSE = "else";

	public static ActionRuleInfo createActionRuleInfo(IlrActionRule rule) throws IlrObjectNotFoundException
	{
		return new ActionRuleInfo(rule.getDefinition().getBody());
	}
	
	public int getDefinitionsCount() {
		return definitionsCount;
	}

	public int getIfCount() {
		return ifCount;
	}

	public int getThenCount() {
		return thenCount;
	}

	public int getElseCount() {
		return elseCount;
	}

	public int getCountOfConstantsInDefinitions() {
		return countOfConstantsInDefinitions;
	}

	public int getCountOfConstantsInConditions() {
		return countOfConstantsInConditions;
	}

	@NotBusiness
	public ActionRuleInfo(String body) {
		this.body = preprocess(body);
		parse();
	}

	/**
	 * Process the body string to remove opportunities for string lookup errors
	 * (mainly for semicolons)
	 * 
	 * @param body
	 * @return
	 * @throws Exception
	 */
	private String preprocess(String body) {
		body = body.toLowerCase();
		// Start with replacing string constants, in case it contains single
		// quotes.
		body = body.replaceAll("\"[^\"]*?\"", CST);
		body = body.replaceAll("'[^']+?'", VAR);
		return body;
	}

	/**
	 * Detect: - The number of definition statements - The presence of
	 * conditions (if statement) - The count of actions in the then statement -
	 * The count of actions in the else statement It does assume that the rule
	 * body is properly formed.
	 */
	private void parse() {
		definitionsCount = lookFor(DEFINITIONS, IF);
		if (definitionsCount == 0) {
			definitionsCount = lookFor(DEFINITIONS, THEN);
		}
		countOfConstantsInDefinitions = countConstants();

		ifCount = lookFor(IF, THEN);
		countOfConstantsInConditions = countConstants();

		if (ifCount == 0) {
			thenCount = lookFor(THEN, null);
		} else {
			thenCount = lookFor(THEN, ELSE);
			if (thenCount == 0) {
				thenCount = lookFor(THEN, null);
			} else {
				elseCount = lookFor(ELSE, null);
			}
		}
	}

	/**
	 * Looks for a section between start and stop.
	 * 
	 * @param start
	 * @param stop
	 * @return
	 */
	private int lookFor(String start, String stop) {
		found = null;
		if (body == null) {
			return 0;
		}
		body = body.trim();
		if (!body.startsWith(start)) {
			return 0;
		}
		if (stop == null) {
			found = body;
			return countParts(body);
		} else {
			String[] chunks = body.split("\\s*" + stop + "\\s*");
			if (chunks.length == 1) {
				return 0;
			} else if (chunks.length == 2) {
				body = stop + " " + chunks[1];
				found = chunks[0];
				return countParts(chunks[0]);
			} else {
				body = null;
				return 0;
			}
		}
	}

	/**
	 * Counts statements separated by a semicolon.
	 * 
	 * @param statements
	 * @return
	 */
	private int countParts(String statements) {
		return statements.split(";").length;
	}

	/**
	 * Counts the number of constant strings referenced in the set of statements.
	 * Constant and variable markers are the only capitalized words, so makes it easier to count.
	 * @return
	 */
	private int countConstants() {
		if (found != null) {
			String stripped = found.replace(CST, "");
			return ((found.length() - stripped.length()) / CST.length());
		} else {
			return 0;
		}
	}
}
