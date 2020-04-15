package ru.clenum.model;

import org.apache.maven.plugins.annotations.Parameter;

public class Target {
    @Parameter(property = "packageName", required = true)
    private String packageName;
    @Parameter(property = "directory", required = true)
    private String directory;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }
}
