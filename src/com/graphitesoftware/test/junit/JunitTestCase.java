package com.graphitesoftware.test.junit;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;


public class JunitTestCase {
	private String classname;
    private String name;
    private JunitError error;
    private JunitFailure failure;
    private JunitSkipped skipped;

    @XmlAttribute
    public void setClassname(String classname) { this.classname = classname; }
    @XmlAttribute
    public void setName(String name) { this.name = name; }
    @XmlElement(name = "error")
    public void setError(JunitError error) { this.error = error; }
   
    @XmlElement(name = "failure")
    public void setFailure(JunitFailure failure) { this.failure = failure; }
   
    @XmlElement(name = "skipped")
    public void setSkipped(JunitSkipped skipped) { this.skipped = skipped; }

    public String getClassname() { return this.classname; }
    public String getName() { return this.name; }
    public JunitError getError() { return this.error; }
    public JunitFailure getFailure() { return this.failure; }
    public JunitSkipped getSkipped() { return this.skipped; }
}

