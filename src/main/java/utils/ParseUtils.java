package utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ParseUtils {

    private static final String HEADER_REGEX = ":";
    private static final int START_INDEX = 0;
    private static final String REQUEST_URL_REGEX = "\\?";
    private static final String PARAMETER_PAIR_REGEX = "&";
    private static final String PARAMETER_KEY_VALUE_REGEX = "=";

    public static String parseHeaderValue(String header) {
        return header.substring(header.indexOf(HEADER_REGEX) + 1).trim();
    }

    public static String parseHeaderKey(String header) {
        return header.substring(START_INDEX, header.indexOf(HEADER_REGEX));
    }
    public static Map<String, String> getParameters(String requestUrl) {
        Map<String, String> parameters = new HashMap<>();

        Arrays.stream(requestUrl.split(REQUEST_URL_REGEX)[1].split(PARAMETER_PAIR_REGEX))
                .map(pair -> pair.split(PARAMETER_KEY_VALUE_REGEX))
                .forEach(keyValue -> parameters.put(keyValue[0], keyValue[1]));

        return parameters;
    }

    public static String getUrlPath(String url) {
        return url.split(REQUEST_URL_REGEX)[0];
    }
}
