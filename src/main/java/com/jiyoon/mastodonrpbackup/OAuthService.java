package com.jiyoon.mastodonrpbackup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public class OAuthService {

    private static final String CLIENT_NAME = "Mastodon RP Backup";

    /*
     * 터미널 프로그램이므로 로그인 승인 후
     * 인증 코드를 브라우저 화면에 표시하게 합니다.
     */
    private static final String REDIRECT_URI =
            "urn:ietf:wg:oauth:2.0:oob";

    /*
     * RP 게시글을 읽고, 로그인 계정 확인(verify_credentials)을 위해
     * 계정 조회 권한도 함께 요청합니다.
     */
    private static final String SCOPES = "read:accounts read:statuses";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OAuthService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        this.objectMapper = new ObjectMapper();
    }

    /**
     * 입력받은 마스토돈 서버 주소를 정리합니다.
     *
     * @param input 예: https://mastodon.social
     */
    public String normalizeInstanceUrl(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException(
                    "마스토돈 서버 주소가 비어 있습니다."
            );
        }

        String instanceUrl = input.trim();

        while (instanceUrl.endsWith("/")) {
            instanceUrl = instanceUrl.substring(
                    0,
                    instanceUrl.length() - 1
            );
        }

        URI uri;

        try {
            uri = URI.create(instanceUrl);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "올바른 서버 주소가 아닙니다.",
                    e
            );
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException(
                    "서버 주소는 https://로 시작해야 합니다."
            );
        }

        if (uri.getHost() == null) {
            throw new IllegalArgumentException(
                    "서버 도메인을 찾을 수 없습니다."
            );
        }

        return instanceUrl;
    }

    /**
     * 사용자가 입력한 마스토돈 서버에
     * 현재 프로그램을 OAuth 애플리케이션으로 등록합니다.
     */
    public OAuthApplication registerApplication(
            String instanceUrl
    ) throws IOException, InterruptedException {

        String endpoint =
                instanceUrl + "/api/v1/apps";

        ObjectNode requestBody =
                objectMapper.createObjectNode();

        requestBody.put(
                "client_name",
                CLIENT_NAME
        );

        ArrayNode redirectUris =
                requestBody.putArray("redirect_uris");

        redirectUris.add(REDIRECT_URI);

        requestBody.put(
                "scopes",
                SCOPES
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(30))
                .header(
                        "Accept",
                        "application/json"
                )
                .header(
                        "Content-Type",
                        "application/json"
                )
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                objectMapper.writeValueAsString(
                                        requestBody
                                )
                        )
                )
                .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        checkResponse(response, "애플리케이션 등록");

        JsonNode json =
                objectMapper.readTree(response.body());

        String clientId =
                requiredText(json, "client_id");

        String clientSecret =
                requiredText(json, "client_secret");

        return new OAuthApplication(
                clientId,
                clientSecret
        );
    }

    /**
     * 사용자가 브라우저에서 열 로그인·승인 주소를 생성합니다.
     */
    public String createAuthorizationUrl(
            String instanceUrl,
            OAuthApplication application
    ) {
        Map<String, String> parameters =
                new LinkedHashMap<>();

        parameters.put("response_type", "code");
        parameters.put(
                "client_id",
                application.clientId()
        );
        parameters.put(
                "redirect_uri",
                REDIRECT_URI
        );
        parameters.put("scope", SCOPES);

        return instanceUrl
                + "/oauth/authorize?"
                + encodeForm(parameters);
    }

    /**
     * 가능한 환경에서는 기본 브라우저를 엽니다.
     * 실패해도 예외를 밖으로 던지지 않고 false를 반환합니다.
     */
    public boolean openBrowser(String authorizationUrl) {
        if (!Desktop.isDesktopSupported()) {
            return false;
        }

        Desktop desktop = Desktop.getDesktop();

        if (!desktop.isSupported(
                Desktop.Action.BROWSE
        )) {
            return false;
        }

        try {
            desktop.browse(
                    URI.create(authorizationUrl)
            );

            return true;

        } catch (IOException
                 | IllegalArgumentException
                 | UnsupportedOperationException
                 | SecurityException e) {

            return false;
        }
    }

    /**
     * 브라우저에서 받은 인증 코드를
     * 사용자 액세스 토큰으로 교환합니다.
     */
    public OAuthToken exchangeCodeForToken(
            String instanceUrl,
            OAuthApplication application,
            String authorizationCode
    ) throws IOException, InterruptedException {

        if (authorizationCode == null
                || authorizationCode.isBlank()) {

            throw new IllegalArgumentException(
                    "인증 코드가 비어 있습니다."
            );
        }

        String endpoint =
                instanceUrl + "/oauth/token";

        Map<String, String> parameters =
                new LinkedHashMap<>();

        parameters.put(
                "grant_type",
                "authorization_code"
        );

        parameters.put(
                "code",
                authorizationCode.trim()
        );

        parameters.put(
                "client_id",
                application.clientId()
        );

        parameters.put(
                "client_secret",
                application.clientSecret()
        );

        parameters.put(
                "redirect_uri",
                REDIRECT_URI
        );

        String requestBody =
                encodeForm(parameters);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(30))
                .header(
                        "Accept",
                        "application/json"
                )
                .header(
                        "Content-Type",
                        "application/x-www-form-urlencoded"
                )
                .POST(
                        HttpRequest.BodyPublishers.ofString(
                                requestBody
                        )
                )
                .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        checkResponse(response, "액세스 토큰 발급");

        JsonNode json =
                objectMapper.readTree(response.body());

        return new OAuthToken(
                requiredText(json, "access_token"),
                json.path("token_type")
                        .asText("Bearer"),
                json.path("scope")
                        .asText(SCOPES)
        );
    }

    /**
     * 현재 로그인한 마스토돈 계정의 표시 이름을 가져옵니다.
     * display_name이 비어 있으면 acct를 대신 반환합니다.
     */
    public String getCurrentAccountDisplayName(
            String instanceUrl,
            String accessToken
    ) throws IOException, InterruptedException {

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException(
                    "액세스 토큰이 비어 있습니다."
            );
        }

        String endpoint =
                instanceUrl
                        + "/api/v1/accounts/verify_credentials";

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(30))
                        .header(
                                "Accept",
                                "application/json"
                        )
                        .header(
                                "Authorization",
                                "Bearer " + accessToken
                        )
                        .GET()
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        checkResponse(
                response,
                "로그인 계정 확인"
        );

        JsonNode json =
                objectMapper.readTree(
                        response.body()
                );

        String displayName =
                json.path("display_name")
                        .asText("")
                        .trim();

        if (!displayName.isBlank()) {
            return displayName;
        }

        String acct =
                json.path("acct")
                        .asText("")
                        .trim();

        if (!acct.isBlank()) {
            return acct;
        }

        return "(이름 없음)";
    }

    /**
     * application/x-www-form-urlencoded 형식으로 변환합니다.
     */
    private String encodeForm(
            Map<String, String> parameters
    ) {
        StringBuilder encoded =
                new StringBuilder();

        for (Map.Entry<String, String> entry
                : parameters.entrySet()) {

            if (!encoded.isEmpty()) {
                encoded.append('&');
            }

            encoded.append(
                    encode(entry.getKey())
            );

            encoded.append('=');

            encoded.append(
                    encode(entry.getValue())
            );
        }

        return encoded.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }

    private String requiredText(
            JsonNode json,
            String fieldName
    ) throws IOException {

        String value =
                json.path(fieldName).asText();

        if (value.isBlank()) {
            throw new IOException(
                    "응답 JSON에 "
                            + fieldName
                            + " 값이 없습니다."
            );
        }

        return value;
    }

    private void checkResponse(
            HttpResponse<String> response,
            String operation
    ) throws IOException {

        int statusCode =
                response.statusCode();

        if (statusCode >= 200
                && statusCode < 300) {
            return;
        }

        throw new IOException(
                operation
                        + " 실패\n"
                        + "HTTP 상태 코드: "
                        + statusCode
                        + "\n"
                        + "서버 응답: "
                        + response.body()
        );
    }

    /**
     * 서버에서 발급받은 앱 자격증명입니다.
     */
    public record OAuthApplication(
            String clientId,
            String clientSecret
    ) {
    }

    /**
     * 로그인한 사용자의 OAuth 토큰입니다.
     */
    public record OAuthToken(
            String accessToken,
            String tokenType,
            String scope
    ) {
    }
}