package ru.clenum.service;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import ru.clenum.model.*;
import ru.clenum.template.Const;
import ru.clenum.utils.Utils;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.*;

public class DatabaseService {

    private final MavenProject project;

    private final Jdbc jdbc;

    public DatabaseService(MavenProject project, Jdbc jdbc) {
        this.project = project;
        this.jdbc = jdbc;
    }

    private Connection getConnection() throws IllegalAccessException, InstantiationException, SQLException, MojoExecutionException, ClassNotFoundException {
        Class<? extends Driver> classDriver = (Class<? extends Driver>) getDriver();
        Properties properties = new Properties();
        properties.put("user", jdbc.getUsername());
        properties.put("password", jdbc.getPassword());
        return classDriver.newInstance().connect(jdbc.getUrl(), properties);
    }

    private Class<?> getDriver() throws ClassNotFoundException, MojoExecutionException {
        URLClassLoader classLoader = getClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            return Class.forName(jdbc.getDriver());
        }
        catch (ClassNotFoundException e) {
            return Thread.currentThread().getContextClassLoader().loadClass(jdbc.getDriver());
        }
    }

    private URLClassLoader getClassLoader() throws MojoExecutionException {
        try {
            List<String> classpathElements = project.getRuntimeClasspathElements();
            classpathElements.addAll(project.getCompileClasspathElements());
            URL urls[] = new URL[classpathElements.size()];

            for (int i = 0; i < urls.length; i++)
                urls[i] = new File(classpathElements.get(i)).toURI().toURL();

            return new URLClassLoader(urls, getClass().getClassLoader());
        }
        catch (Exception e) {
            throw new MojoExecutionException("Couldn't create a classloader.", e);
        }
    }

    public List<Table> getTablesForSchema(Schema schema) throws SQLException {
        StringJoiner excludeJoiner = new StringJoiner(", ");
        if (schema.getExcludeTables() != null) {
            for (String excludeTable : schema.getExcludeTables()) {
                excludeJoiner.add(excludeTable);
            }
        }
        String tablesSelect = "select ist.table_name from information_schema.tables as ist where ist.table_schema = ? and ist.table_name not in (?)";
        try (Connection connection = getConnection()) {
            PreparedStatement tablesPS = connection.prepareStatement(tablesSelect);
            tablesPS.setString(1, schema.getName());
            tablesPS.setString(2, excludeJoiner.toString());
            ResultSet tablesRS = tablesPS.executeQuery();
            List<Table> tableList = new ArrayList<>();
            while (tablesRS.next()) {
                Table table = new Table();
                table.setSchema(schema.getName());
                table.setTable(tablesRS.getString(1));
                tableList.add(table);
            }
            return tableList;
        } catch (MojoExecutionException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public String getPrimaryNameForTable(Table table) throws SQLException {
        String pkNameQuery = "select kcu.column_name from information_schema.table_constraints as tc join information_schema.key_column_usage kcu on tc.constraint_name = kcu.constraint_name and tc.constraint_schema = kcu.constraint_schema where tc.constraint_type = 'PRIMARY KEY' and tc.table_schema = ? and tc.table_name = ?;";
        try (Connection connection = getConnection()) {
            PreparedStatement pkNamePS = connection.prepareStatement(pkNameQuery);
            pkNamePS.setString(1, table.getSchema());
            pkNamePS.setString(2, table.getTable());
            String pkName = "";
            ResultSet pkNameRS = pkNamePS.executeQuery();
            while (pkNameRS.next()) {
                pkName = pkNameRS.getString(1);
            }
            return pkName;
        } catch (MojoExecutionException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ColumnInfo> getColumnInfoListForTable(Table table) throws SQLException {
        String informationQuery = "select * from information_schema.columns as c where c.table_schema = ? and c.table_name = ?;";
        try (Connection connection = getConnection()) {
            PreparedStatement infoPreparedStatement = connection.prepareStatement(informationQuery);
            infoPreparedStatement.setString(1, table.getSchema());
            infoPreparedStatement.setString(2, table.getTable());
            ResultSet infoResultSet = infoPreparedStatement.executeQuery();
            List<ColumnInfo> columnInfoList = new ArrayList<>();
            while (infoResultSet.next()) {
                String columnName = infoResultSet.getString("column_name");
                String dataType = infoResultSet.getString("data_type");
                ColumnInfo columnInfo = new ColumnInfo(columnName, dataType.toLowerCase());
                columnInfoList.add(columnInfo);
            }
            return columnInfoList;
        } catch (MojoExecutionException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    //Разделить генерацию и sql
    public String getResultGenerateStringForTable(Table table, String pkName, List<ColumnInfo> columnInfoList, ImportSettings importSettings, String sourceName, String packageName) throws SQLException {
        String select = "select * from " + table.getSchema() + ".\"" + table.getTable() + "\" order by " + pkName + " asc;";
        try (Connection connection = getConnection()) {
            ResultSet selectResult = connection.createStatement().executeQuery(select);
            StringBuilder enumBuilder = new StringBuilder();
            StringBuilder fieldBuilder = new StringBuilder();
            StringBuilder getterBuilder = new StringBuilder();
            StringBuilder constructorParamsBuilder = new StringBuilder();
            StringBuilder constructorBodyBuilder = new StringBuilder();
            StringBuilder refFillBuilder = new StringBuilder();
            String pkType = "";
            while (selectResult.next()) {
                boolean flagIsFirst = selectResult.isFirst();
                Optional<ColumnInfo> optionalEnumNameField = columnInfoList.stream().filter(c -> c.getColumnName().equals(importSettings.getEnumFieldName())).findFirst();
                if (optionalEnumNameField.isPresent()) {
                    String  enumNameField = selectResult.getString(importSettings.getEnumFieldName());
                    if (Character.isDigit(enumNameField.charAt(0))) {
                        enumNameField = "_" + enumNameField;
                    }
                    enumBuilder.append(enumNameField);
                } else {
                    String enumNameField = table.getTable().toUpperCase() + "_" + selectResult.getString(pkName);
                    if (Character.isDigit(enumNameField.charAt(0))) {
                        enumNameField = "_" + enumNameField;
                    }
                    enumBuilder.append(enumNameField);
                }
                enumBuilder.append("(");
                for (ColumnInfo columnInfo : columnInfoList) {

                    if (columnInfo.getColumnName().equals(pkName)) {

                        String fieldName = Utils.removeUnderscore(pkName);

                        if (flagIsFirst) {
                            constructorBodyBuilder.append("this.").append(fieldName).append(" = ").append(fieldName).append(";\n");
                            refFillBuilder.append("ref.put(\"").append(fieldName).append("\", value.get").append(Utils.toUpperCaseFirstChar(fieldName)).append("());\n");
                        }

                        switch (columnInfo.getDataType()) {
                            case "integer": {
                                Integer data = selectResult.getInt(pkName);
                                if (data == null) {
                                    data = 0;
                                }
                                enumBuilder.append(data.toString());
                                if (flagIsFirst) {
                                    fillFirstPK(fieldBuilder, getterBuilder, constructorParamsBuilder, fieldName, Const.INTEGER);
                                }
                                pkType = "Integer";
                                break;
                            }
                            case "character varying":{}
                            case "text": {
                                String data = selectResult.getString(pkName);
                                if (data == null) {
                                    data = "";
                                }
                                data = data.replace("\"", "'");
                                enumBuilder.append("\"").append(data).append("\"");
                                if (flagIsFirst) {
                                    fillFirstPK(fieldBuilder, getterBuilder, constructorParamsBuilder, fieldName, Const.STRING);
                                }
                                pkType = "String";
                                break;
                            }
                            case "bigint": {
                                Long data = selectResult.getLong(pkName);
                                if (data == null) {
                                    data = 0L;
                                }
                                enumBuilder.append(data.toString());
                                if (flagIsFirst) {
                                    fillFirstPK(fieldBuilder, getterBuilder, constructorParamsBuilder, fieldName, Const.LONG);
                                }
                                pkType = "Long";
                                break;
                            }
                            case "numeric": {
                                BigDecimal data = selectResult.getBigDecimal(pkName);
                                if (data == null) {
                                    data = BigDecimal.ZERO;
                                }
                                enumBuilder.append(data.toString());
                                if (flagIsFirst) {
                                    fillFirstPK(fieldBuilder, getterBuilder, constructorParamsBuilder, fieldName, Const.BIG_DECIMAL);
                                }
                                pkType = "BigDecimal";
                                break;
                            }
                            case "boolean": {
                                Boolean data = selectResult.getBoolean(pkName);
                                if (data == null) {
                                    data = false;
                                }
                                enumBuilder.append(data.toString());
                                if (flagIsFirst) {
                                    fillFirstPK(fieldBuilder, getterBuilder, constructorParamsBuilder, fieldName, Const.BOOLEAN);
                                }
                                pkType = "Boolean";
                                break;
                            }
                        }
                    }
                }
                for (int i = 0; i < columnInfoList.size(); i++) {

                    System.out.println(columnInfoList.get(i).getColumnName());
                    System.out.println(constructorParamsBuilder);
                    if (!columnInfoList.get(i).getColumnName().equals(pkName)
                            && !columnInfoList.get(i).getColumnName().equals(importSettings.getEnumFieldName())) {

                        if (table.getFieldsRules() != null) {
                            FieldsRules fieldsRules = table.getFieldsRules();
                            String[] fields = fieldsRules.getFields().split(",");
                            boolean contain = false;
                            if (FieldsRules.INCLUDE.equalsIgnoreCase(fieldsRules.getType())) {
                                for (String field : fields) {
                                    field = field.trim();
                                    if (field.equalsIgnoreCase(columnInfoList.get(i).getColumnName())) {
                                        contain = true;
                                    }
                                }
                                if (!contain) {
                                    continue;
                                }
                            } else if (FieldsRules.EXCLUDE.equalsIgnoreCase(fieldsRules.getType())) {
                                for (String field : fields) {
                                    field = field.trim();
                                    if (field.equalsIgnoreCase(columnInfoList.get(i).getColumnName())) {
                                        contain = true;
                                    }
                                }
                                if (contain) {
                                    continue;
                                }
                            }
                        }

                        String fieldName = Utils.removeUnderscore(columnInfoList.get(i).getColumnName());

                        if (Character.isDigit(fieldName.charAt(0))) {
                            fieldName = "_" + fieldName;
                        }

                        if (flagIsFirst) {
                            constructorBodyBuilder.append("        ").append("this.").append(fieldName).append(" = ").append(fieldName).append(";\n");
                            refFillBuilder.append("            ").append("ref.put(\"").append(fieldName).append("\", value.get").append(Utils.toUpperCaseFirstChar(fieldName)).append("());\n");
                        }

                        switch (columnInfoList.get(i).getDataType()) {
                            case "integer": {
                                Integer data = selectResult.getInt(columnInfoList.get(i).getColumnName());
                                if (data == null) {
                                    data = 0;
                                }
                                enumBuilder.append(", ").append(data.toString());
                                if (flagIsFirst) {
                                    fillFirst(fieldBuilder, getterBuilder, constructorParamsBuilder, fieldName, Const.INTEGER);
                                }
                                break;
                            }
                            case "character varying":{}
                            case "text": {
                                String data = selectResult.getString(columnInfoList.get(i).getColumnName());
                                if (data == null) {
                                    data = "";
                                }
                                data = data.replace("\"", "'");
                                enumBuilder.append(", ").append("\"").append(data).append("\"");
                                if (flagIsFirst) {
                                    fillFirst(fieldBuilder, getterBuilder, constructorParamsBuilder, fieldName, Const.STRING);
                                }
                                break;
                            }
                            case "bigint": {
                                Long data = selectResult.getLong(columnInfoList.get(i).getColumnName());
                                if (data == null) {
                                    data = 0L;
                                }
                                enumBuilder.append(", ").append(data.toString());
                                if (flagIsFirst) {
                                    fillFirst(fieldBuilder, getterBuilder, constructorParamsBuilder, fieldName, Const.LONG);
                                }
                                break;
                            }
                            case "numeric": {
                                BigDecimal data = selectResult.getBigDecimal(columnInfoList.get(i).getColumnName());
                                if (data == null) {
                                    data = BigDecimal.ZERO;
                                }
                                enumBuilder.append(", BigDecimal.valueOf(").append(data.toString()).append(")");
                                if (flagIsFirst) {
                                    fillFirst(fieldBuilder, getterBuilder, constructorParamsBuilder, fieldName, Const.BIG_DECIMAL);
                                }
                                break;
                            }
                            case "boolean": {
                                Boolean data = selectResult.getBoolean(columnInfoList.get(i).getColumnName());
                                if (data == null) {
                                    data = false;
                                }
                                enumBuilder.append(", ").append(data.toString());
                                if (flagIsFirst) {
                                    fillFirst(fieldBuilder, getterBuilder, constructorParamsBuilder, fieldName, Const.BOOLEAN);
                                }
                                break;
                            }
                        }
                    }
                }
                enumBuilder.append(")");
                if (selectResult.isLast()) {
                    enumBuilder.append(";");
                } else {
                    enumBuilder.append(",\n    ");
                }
            }

            String resultString = Const.TEMPLATE.replaceAll("\\{package}", packageName);
            resultString = resultString.replaceAll("\\{enumName}", sourceName);
            resultString = resultString.replaceAll("\\{enumeration}", enumBuilder.toString());
            resultString = resultString.replaceAll("\\{fields}", fieldBuilder.toString());
            resultString = resultString.replaceAll("\\{pkType}", pkType);
            resultString = resultString.replaceAll("\\{pkName}", Utils.removeUnderscore(pkName));
            resultString = resultString.replaceAll("\\{getters}", getterBuilder.toString());
            resultString = resultString.replaceAll("\\{constructorParams}", constructorParamsBuilder.toString());
            resultString = resultString.replaceAll("\\{constructorBody}", constructorBodyBuilder.toString());
            resultString = resultString.replaceAll("\\{refFill}", refFillBuilder.toString());
            return resultString;
        } catch (MojoExecutionException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void fillFirst(StringBuilder fieldBuilder, StringBuilder getterBuilder, StringBuilder constructorParamsBuilder, String fieldName, String type) {
        fieldBuilder.append("    ").append("private ").append(type).append(" ").append(fieldName).append(";\n");
        getterBuilder.append("    ").append("public ").append(type).append(" get").append(Utils.toUpperCaseFirstChar(fieldName)).append("() {return this.").append(fieldName).append(";}\n");
        constructorParamsBuilder.append(", ").append(type).append(" ").append(fieldName);
    }

    private void fillFirstPK(StringBuilder fieldBuilder, StringBuilder getterBuilder, StringBuilder constructorParamsBuilder, String fieldName, String type) {
        fieldBuilder.append("private ").append(type).append(" ").append(fieldName).append(";\n");
        getterBuilder.append("public ").append(type).append(" get").append(Utils.toUpperCaseFirstChar(fieldName)).append("() {return this.").append(fieldName).append(";}\n");
        constructorParamsBuilder.append(type).append(" ").append(fieldName);
    }
}
