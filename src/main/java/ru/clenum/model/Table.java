package ru.clenum.model;

import org.apache.maven.plugins.annotations.Parameter;

public class Table {
    @Parameter(property = "schema", required = true)
    private String schema;
    @Parameter(property = "table", required = true)
    private String table;
    @Parameter(property = "enumName")
    private String enumName;// Custom name for enum class in generated java class instead table name.
    @Parameter(property = "fieldsRules")
    private FieldsRules fieldsRules;

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getEnumName() {
        return enumName;
    }

    public void setEnumName(String enumName) {
        this.enumName = enumName;
    }

    public FieldsRules getFieldsRules() {
        return fieldsRules;
    }

    public void setFieldsRules(FieldsRules fieldsRules) {
        this.fieldsRules = fieldsRules;
    }
}
