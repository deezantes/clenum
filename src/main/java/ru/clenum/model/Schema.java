package ru.clenum.model;

import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;

public class Schema {
    @Parameter(property = "name", required = true)
    private String name;
    @Parameter(property = "excludeTables", required = true)
    private List<String> excludeTables;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getExcludeTables() {
        return excludeTables;
    }

    public void setExcludeTables(List<String> excludeTables) {
        this.excludeTables = excludeTables;
    }
}
