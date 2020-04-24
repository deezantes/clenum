package ru.clenum.model;

import org.apache.maven.plugins.annotations.Parameter;

public class FieldsRules {

    public static final String INCLUDE = "include";
    public static final String EXCLUDE = "exclude";

    @Parameter(property = "type", required = true, defaultValue = INCLUDE)
    private String type;
    @Parameter(property = "fields", required = true)
    private String fields;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFields() {
        return fields;
    }

    public void setFields(String fields) {
        this.fields = fields;
    }
}
