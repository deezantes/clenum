package ru.clenum.utils;

public class Utils {

    public static String removeUnderscore(String string) {
        String[] resultArray = string.split("_");
        String result = "";
        for (int i = 0; i < resultArray.length; i++) {
            if (i == 0) {
                result = resultArray[i];
            } else {
                result = result + toUpperCaseFirstChar(resultArray[i]);
            }
        }
        return result;
    }

    public static String toUpperCaseFirstChar(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }
}
