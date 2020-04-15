package ru.clenum.model;

import org.apache.maven.plugins.annotations.Parameter;

public class Jdbc {
    @Parameter(property = "driver", required = true)
    private String driver;
    @Parameter(property = "url", required = true)
    private String url;
    @Parameter(property = "username", required = true)
    private String username;
    @Parameter(property = "password", required = true)
    private String password;

    public String getDriver() {
        return driver;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
