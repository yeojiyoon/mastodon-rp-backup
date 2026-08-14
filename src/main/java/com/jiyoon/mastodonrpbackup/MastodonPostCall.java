package com.jiyoon.mastodonrpbackup;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MastodonPostCall {

    private final HttpClient httpClient;

    public MastodonPostCall() {
        this.httpClient = HttpClient.newHttpClient();
    }

    public String getContext(
            String postUrl,
            String accessToken
    ) throws IOException, InterruptedException {

        String apiUrl =
                buildStatusApiUrl(
                        postUrl,
                        "/context"
                );

        return fetch(apiUrl, accessToken);
    }

    /**
     * 시작 게시글(주소를 붙여넣은 그 글) 자체를 가져옵니다.
     * context API는 답글만 주고 시작 글은 주지 않기 때문에
     * 이 메서드로 따로 가져와 백업 맨 위에 붙입니다.
     */
    public String getStatus(
            String postUrl,
            String accessToken
    ) throws IOException, InterruptedException {

        String apiUrl =
                buildStatusApiUrl(
                        postUrl,
                        ""
                );

        return fetch(apiUrl, accessToken);
    }

    private String buildStatusApiUrl(
            String postUrl,
            String suffix
    ) {
        URI postUri = URI.create(postUrl);

        String instanceUrl =
                postUri.getScheme()
                        + "://"
                        + postUri.getHost();

        String path = postUri.getPath();

        String statusId =
                path.substring(
                        path.lastIndexOf('/') + 1
                );

        return instanceUrl
                + "/api/v1/statuses/"
                + statusId
                + suffix;
    }

    private String fetch(
            String apiUrl,
            String accessToken
    ) throws IOException, InterruptedException {

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
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

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw new IOException(
                    "게시글 요청 실패: HTTP "
                            + response.statusCode()
                            + "\n"
                            + response.body()
            );
        }

        return response.body();
    }
}