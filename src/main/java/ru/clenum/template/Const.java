package ru.clenum.template;

public class Const {
    public static final String TEMPLATE = "package {package};\n" +
            "\n" +
            "import java.io.Serializable;\n" +
            "import java.math.BigDecimal;\n" +
            "import java.util.*;\n" +
            "\n" +
            "public enum {enumName} implements Serializable {\n" +
            "    {enumeration}\n" +
            "\n" +
            "    {fields}" +
            "    private static final Map<{pkType}, {enumName}> ENUM_MAP;\n" +
            "\n" +
            "    {enumName}({constructorParams}){\n" +
            "        {constructorBody}" +
            "    }\n" +
            "\n" +
            "    static {\n" +
            "        Map<{pkType}, {enumName}> map = new HashMap<>();\n" +
            "        for ({enumName} value : {enumName}.values()) {\n" +
            "            map.put(value.{pkName}, value);\n" +
            "        }\n" +
            "        ENUM_MAP = Collections.unmodifiableMap(map);\n" +
            "    }\n" +
            "\n" +
            "    {getters}" +
            "\n" +
            "    public static {enumName} getByKey({pkType} key){\n" +
            "        return ENUM_MAP.get(key);\n" +
            "    }\n" +
            "\n" +
            "    public static List<Map<String, Object>> getReference(){\n" +
            "        List<Map<String, Object>> result = new ArrayList<>();\n" +
            "        for ({enumName} value : {enumName}.values()) {\n" +
            "            Map<String, Object> ref = new HashMap<>();\n" +
            "            {refFill}" +
            "            result.add(ref);\n" +
            "        }\n" +
            "        return result;\n" +
            "    }\n" +
            "}";

    public static final String ENUMS = "enums";
    public static final String INTEGER = "Integer";
    public static final String LONG = "Long";
    public static final String BOOLEAN = "Boolean";
    public static final String STRING = "String";
    public static final String BIG_DECIMAL = "BigDecimal";
}
