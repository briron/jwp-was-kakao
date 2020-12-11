package webserver.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import webserver.http.session.*;
import webserver.http.utils.CookieParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HttpRequest {

    private final HttpSessionManager sessionManager;
    private final HttpRequestLine requestLine;
    private final List<HttpHeader> headers;
    private final List<Cookie> cookies;
    private final String body;

    public static Builder builder() {
        return new Builder();
    }

    private HttpRequest(HttpRequestLine requestLine, List<HttpHeader> headers, String body, HttpSessionManager sessionManager) {
        this.requestLine = requestLine;
        this.headers = headers;
        this.cookies = setCookies(headers);
        this.body = body;
        this.sessionManager = sessionManager;
    }

    private List<Cookie> setCookies(List<HttpHeader> headers) {
        HttpHeader cookieHeader = headers.stream()
                .filter(it -> "Cookie".equalsIgnoreCase(it.getKey()))
                .findFirst()
                .orElse(null);

        if (cookieHeader == null) return new ArrayList<>();

        CookieParser parser = new CookieParser();
        return parser.parse(cookieHeader.getValue());
    }

    public HttpSession getSession() {
        return getSession(true);
    }

    public HttpSession getSession(boolean createIfAbsent) {
        if (sessionManager == null) throw new HttpSessionException("WebServer 구동시 session 사용설정이 필요합니다");

        String sessionId = getCookie(HttpSessions.SESSION_ID_COOKIE_HEADER);
        HttpSession session = sessionManager.getSession(SessionId.of(sessionId)).orElse(null);
        if (session == null && createIfAbsent) return sessionManager.createSession();
        return session;
    }

    public List<HttpHeader> getHeaders() {
        return headers;
    }

    public List<HttpRequestParam> getParams() {
        return requestLine.getParams();
    }

    public HttpRequestParam getParam(String name) {
        return requestLine.getParams().stream()
                .filter(it -> it.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(requestLine).append("\n");
        headers.forEach(header -> sb.append(header).append("\n"));
        return sb.toString();
    }

    public String getCookie(String cookieName) {
        return cookies.stream()
                .filter(it -> it.getName().equalsIgnoreCase(cookieName))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    public String getPath() {
        return requestLine.getUri().getPath();
    }

    public String getRequestLine() {
        return requestLine.toString();
    }

    public HttpMethod getMethod() {
        return requestLine.getMethod();
    }

    public String getBody() {
        return body;
    }

    static class Builder {
        private Logger logger = LoggerFactory.getLogger(Builder.class);

        private HttpRequestLine requestLine;
        private List<HttpHeader> headers = new ArrayList<>();
        private String body;
        private HttpSessionManager sessionManager;

        public Builder requestLine(HttpRequestLine requestLine) {
            this.requestLine = requestLine;
            return this;
        }

        public Builder headers(List<HttpHeader> headers) {
            this.headers = headers;
            return this;
        }

        public Builder headers(HttpHeader... headers) {
            this.headers = Arrays.asList(headers);
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public HttpRequest build() {
            if (requestLine == null) throw new IllegalArgumentException("request line 이 비어 있습니다");
            HttpRequest httpRequest = new HttpRequest(requestLine, headers, body == null ? "" : body, sessionManager);

            logger.debug("---- request-line ----");
            logger.debug(httpRequest.getRequestLine());
            logger.debug("---- request-header ----");
            httpRequest.getHeaders().forEach(it -> logger.debug(it.toString()));
            logger.debug("---- reqeust-body ----");
            logger.debug(httpRequest.getBody());

            return httpRequest;
        }

        public Builder sessionManager(HttpSessionManager sessionManager) {
            this.sessionManager = sessionManager;
            return this;
        }
    }

}

