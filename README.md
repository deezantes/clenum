# clenum
Java enum generator from database dictionary

## Example
```
<profile>
    <id>clenum</id>
    <build>
        <plugins>
            <plugin>
                <groupId>ru.clenum</groupId>
                <artifactId>clenum</artifactId>
                <version>1.0.0</version>
                <executions>
                    <execution>
                        <id>clenum</id>
                        <phase>generate-sources</phase>
                        <goals>
                            <goal>generate</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <target>
                        <packageName>
                            {...}
                        </packageName>
                        <directory>
                            target/generated-sources/enums/
                        </directory>
                    </target>
                    <jdbc>
                        <driver>org.postgresql.Driver</driver>
                        <url>jdbc:postgresql://${host}:${port}/${db}</url>
                        <username>${login}</username>
                        <password>${password}</password>
                    </jdbc>
                    <tables>
                        <table>
                            <schema>
                                {schema_name}
                            </schema>
                            <table>
                                {table_name}
                            </table>
                            --not required
                            <fieldsRules>
                                <type>
                                    include/exclude
                                </type>
                                <fields>
                                    {table fields, separated by comma. PK field always include, ignored this property}
                                </fields>
                            </fieldsRules>
                            --not required
                            <enumName>
                                {Custom name for enum class in generated java class instead table name.}
                            </enumName>
                        </table>
                    </tables>
                    <importSettings>
                        --not required
                        <enumFieldName>
                            {Text. Column name in dictionary table, that will generate in java enum class as field name. Will generate TABLE_NAME + PK_VALUE, if null. One name for all tables.}
                        </enumFieldName>
                        --not required
                        <withUnderscore>
                            {Boolean. Generate enum java class names with/without underscore.}
                        </withUnderscore>
                    </importSettings>
                </configuration>
            </plugin>
        </plugins>
    </build>
</profile>
```