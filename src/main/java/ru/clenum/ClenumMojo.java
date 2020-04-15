package ru.clenum;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import ru.clenum.model.*;
import ru.clenum.service.DatabaseService;
import ru.clenum.template.Const;
import ru.clenum.utils.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.*;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true, requiresDependencyResolution = ResolutionScope.COMPILE)
public class ClenumMojo extends AbstractMojo {

    private final Log logger = getLog();

    @Parameter(property = "jdbc", required = true)
    private Jdbc jdbc = new Jdbc();

    @Parameter(property = "tables", required = true)
    private List<Table> tables;

    @Parameter(property = "schemas", required = true)
    private List<Schema> schemas;

    @Parameter(property = "target", required = true)
    private Target target;

    @Parameter(property = "importSettings", required = true)
    private ImportSettings importSettings;

    @Component
    private MavenProject project;

    private DatabaseService databaseService;

    @Override
    public void execute() {
        databaseService = new DatabaseService(project, jdbc);
        try {
            if (schemas != null && !schemas.isEmpty()) {
                logger.info("Start generate by schemas");
                generateBySchemas();
            }
            if (tables != null && !tables.isEmpty()) {
                logger.info("Start generate by tables");
                generateByTables();
            }
        } catch (SQLException e) {
            logger.error(e);
        }
    }

    private void generateBySchemas() throws SQLException {
        File dir = makeDir();
        for (Schema schema : schemas) {
            List<Table> tableList = databaseService.getTablesForSchema(schema);
            logger.info("Found tables: " + tableList.size());
            for (Table table : tableList) {
                parseTable(table, dir);
            }
        }
    }

    private void generateByTables() throws SQLException {
        File dir = makeDir();
        for (Table table : tables) {
            parseTable(table, dir);
        }
    }

    private File makeDir() {
        String dirPath = target.getDirectory() + (target.getDirectory().endsWith("/") ? "" : "/") + target.getPackageName().replaceAll("\\.", "/");
        File directory = new File(target.getDirectory());
        if (directory.exists()) {
            try {
                FileUtils.cleanDirectory(directory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        File dir = new File(dirPath);
        if (!dir.exists()) {
            String[] dirPathArray = dirPath.split("/");
            String pathForIteration = "";
            for (String dirPathPart : dirPathArray) {
                File dirPartFile = new File(pathForIteration.isEmpty() ? dirPathPart : pathForIteration + File.separator + dirPathPart);
                dirPartFile.mkdir();
                pathForIteration = dirPartFile.getPath();
            }
        } else {
            dir.mkdir();
        }
        return dir;
    }

    private String getSourceName(Table table) {
        String sourceName = table.getEnumName() != null ? table.getEnumName() : table.getTable();
        if (table.getEnumName() == null && importSettings != null) {
            if (!Boolean.TRUE.equals(importSettings.getWithUnderscore())) {
                String[] underscoreSplit = sourceName.split("_");
                sourceName = "";
                for (String underscorePart : underscoreSplit) {
                    sourceName = sourceName + Utils.toUpperCaseFirstChar(underscorePart);
                }
            }
        }
        return Utils.toUpperCaseFirstChar(sourceName);
    }

    private void parseTable(Table table, File dir) throws SQLException {
        String sourceName = getSourceName(table);
        if (Character.isDigit(sourceName.charAt(0))) {
            sourceName = "_" + sourceName;
        }
        File fileDir;
        String packageName = target.getPackageName();
        if (table.getSchema() != null) {
            packageName = packageName + "." + table.getSchema() + "." + Const.ENUMS;
            fileDir = new File(dir.getPath() + File.separator + table.getSchema());
            fileDir.mkdir();
            fileDir = new File(fileDir.getPath() + File.separator + Const.ENUMS);
            fileDir.mkdir();
        } else {
            packageName = packageName + "." + Const.ENUMS;
            fileDir = new File(dir.getPath() + File.separator + Const.ENUMS);
            fileDir.mkdir();
        }
        File eFile = new File(fileDir.getPath() + File.separator + sourceName + ".java");
        try (FileWriter writer = new FileWriter(eFile, true)) {
            String pkName = databaseService.getPrimaryNameForTable(table);
            List<ColumnInfo> columnInfoList = databaseService.getColumnInfoListForTable(table);
            if (!columnInfoList.isEmpty() && !pkName.isEmpty()) {
                String resultString = databaseService.getResultGenerateStringForTable(table, pkName, columnInfoList, importSettings, sourceName, packageName);
                writer.write(resultString);
                writer.flush();
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }
}
