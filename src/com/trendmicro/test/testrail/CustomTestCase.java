package com.trendmicro.test.testrail;

import java.util.ArrayList;

import org.codehaus.jackson.annotate.JsonProperty;


public class CustomTestCase extends com.rmn.testrail.entity.TestCase {
	
	private boolean isNew = false;
	
	public CustomTestCase() {
		super();

	}
	
	
	
    public boolean isNew() {
		return isNew;
	}
	public void setNew(boolean isNew) {
		this.isNew = isNew;
	}

	@JsonProperty("custom_our_description")
    private String custom_our_description;
    public String getCustomOurDescription() { return custom_our_description; }
    public void setCustomOurDescription(String description) { this.custom_our_description = description; }

	
    @JsonProperty("custom_preconds")
    private String custom_preconds;
    public String getCustomPreconds() { return custom_preconds; }
    public void setCustomPreconds(String preconds) { this.custom_preconds = preconds; }


    @JsonProperty("template_id")
    private Integer templateId;
    public Integer getCustomTemplateId() { return templateId; }
    public void setCustomTemplateId(Integer template) { this.templateId = template; }

    @JsonProperty("custom_steps")
    private String custom_steps;
    public String getCustomSteps() { return custom_steps; }
    public void setCustomSteps(String steps) { this.custom_steps = steps; }

	
    @JsonProperty("custom_expected")
    private String custom_expected;
    public String getCustomExpected() { return custom_expected; }
    public void setCustomExpected(String expected) { this.custom_expected = expected; }
}
