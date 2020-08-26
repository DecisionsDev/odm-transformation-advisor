package com.ibm.odm.ota;

import ilog.rules.bom.annotations.NotBusiness;
import ilog.rules.teamserver.brm.IlrProjectElement;

/**
 * Captures the content of one report item.
 * 
 * @author pberland@us.ibm.com
 *
 */
@NotBusiness
public class ReportElement {

	// What type of issue the element reports.
	private String type;

	// Which rule project or context the issue was found.
	private String where;
	
	// Which rule project branch the issue was found.
	private String branch;

	// What the issue is about.
	private String what;

	// Which rule project element has the issue (can be null if issue is not
	// related to a project element).
	// When provided, this reference is used to point back to the element in the
	// DC repository through its permalink.
	private IlrProjectElement element;

	public ReportElement(String type, String where, String branch, String what,
			IlrProjectElement element) {
		this.type = type;
		this.where = where;
		this.branch = branch;
		this.what = what;
		this.element = element;
	}

	public String getType() {
		return type;
	}

	public String getWhere() {
		return where;
	}

	public String getBranch() {
		return branch;
	}

	public String getWhat() {
		return what;
	}

	public IlrProjectElement getElement() {
		return element;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((branch == null) ? 0 : branch.hashCode());
		result = prime * result + ((element == null) ? 0 : element.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((what == null) ? 0 : what.hashCode());
		result = prime * result + ((where == null) ? 0 : where.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReportElement other = (ReportElement) obj;
		if (branch == null) {
			if (other.branch != null)
				return false;
		} else if (!branch.equals(other.branch))
			return false;
		if (element == null) {
			if (other.element != null)
				return false;
		} else if (!element.equals(other.element))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (what == null) {
			if (other.what != null)
				return false;
		} else if (!what.equals(other.what))
			return false;
		if (where == null) {
			if (other.where != null)
				return false;
		} else if (!where.equals(other.where))
			return false;
		return true;
	}

}
