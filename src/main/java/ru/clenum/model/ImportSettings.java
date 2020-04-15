package ru.clenum.model;

import org.apache.maven.plugins.annotations.Parameter;

public class ImportSettings {
    @Parameter(property = "enumNameField", required = true)
    private String enumFieldName;//Column name in dictionary table, that will generate in java enum class as field name. Will generate TABLE_NAME + PK_VALUE, if null. One name for all tables.
    @Parameter(property = "withUnderscore", required = true)
    private Boolean withUnderscore;//Generate enum java class names with/without underscore.

    public String getEnumFieldName() {
        return enumFieldName;
    }

    public void setEnumFieldName(String enumFieldName) {
        this.enumFieldName = enumFieldName;
    }

    public Boolean getWithUnderscore() {
        return withUnderscore;
    }

    public void setWithUnderscore(Boolean withUnderscore) {
        this.withUnderscore = withUnderscore;
    }
}
