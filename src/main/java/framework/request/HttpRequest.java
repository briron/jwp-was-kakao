package framework.request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.IOUtils;
import utils.ParseUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static framework.common.HttpHeaders.CONTENT_LENGTH;

public class HttpRequest {

    private static final Logger logger = LoggerFactory.getLogger(HttpRequest.class);

    private static final String REQUEST_FIRST_LINE_REGEX = " ";
    private static final String DEFAULT_LINE = "";
    private static final int METHOD_INDEX = 0;
    private static final int URL_INDEX = 1;
    private static final int VERSION_INDEX = 2;

    private HttpMethod method;
    private String url;
    private String version;
    private String path;

    private final Map<String, String> headers;
    private final Map<String, String> parameters;

    private HttpRequest(InputStream in) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        getMethodAndUrl(reader);
        headers = makeHeaders(reader);
        parameters = makeParameters(reader);
    }

    public static HttpRequest of(InputStream in) {
        return new HttpRequest(in);
    }

    private Map<String, String> makeHeaders(BufferedReader reader) {
        List<String> lines = readLines(reader);
        return lines.stream()
                .skip(1)
                .limit(lines.size() - 2)
                .collect(Collectors.toMap(ParseUtils::parseHeaderKey, ParseUtils::parseHeaderValue));
    }

    private void getMethodAndUrl(BufferedReader reader) {
        String[] lines = readLine(reader).split(REQUEST_FIRST_LINE_REGEX);
        method = HttpMethod.of(lines[METHOD_INDEX]);
        url = lines[URL_INDEX];
        version = lines[VERSION_INDEX];
        path = ParseUtils.getUrlPath(url);
    }

    private List<String> readLines(BufferedReader reader) {
        List<String> lines = new ArrayList<>();
        lines.add(readLine(reader));

        while (lines.get(lines.size() - 1) != null &&
                !lines.get(lines.size() - 1).isEmpty()) {
            lines.add(readLine(reader));
        }

        return lines;
    }

    private String readLine(BufferedReader reader) {
        try {
            String line = reader.readLine();
            logger.debug(line);
            return line;
        } catch (IOException e) {
            logger.error(e.getMessage());
            return DEFAULT_LINE;
        }
    }

    private Map<String, String> makeParameters(BufferedReader reader) {
        try {
            Map<String, String> parameters = new HashMap<>();

            if (ParseUtils.containRequestUrlRegex(url)) {
                parameters.putAll(ParseUtils.getParameters(ParseUtils.getParameterPairs(url)));
            }

            if (method.equals(HttpMethod.POST)) {
                String body = IOUtils.readData(reader, Integer.parseInt(headers.get(CONTENT_LENGTH.getHeader())));
                parameters.putAll(ParseUtils.getParameters(body));
            }

            return parameters;

        } catch (IOException e) {
            logger.error(e.getMessage());
            return new HashMap<>();
        }
    }


    public HttpMethod getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }
}
