package com.jiyoon.mastodonrpbackup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class MastodonContextParser {

    private final ObjectMapper objectMapper;

    public MastodonContextParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Context JSON에서 descendants를 읽어
     * 게시글 목록으로 변환합니다.
     */
    public List<RpPost> parse(String contextJson)
            throws IOException {

        if (contextJson == null || contextJson.isBlank()) {
            throw new IllegalArgumentException(
                    "파싱할 JSON 문자열이 비어 있습니다."
            );
        }

        JsonNode root = objectMapper.readTree(contextJson);
        JsonNode descendants = root.path("descendants");

        if (!descendants.isArray()) {
            throw new IOException(
                    "descendants가 배열 형식이 아닙니다."
            );
        }

        List<RpPost> posts = new ArrayList<>();

        for (JsonNode status : descendants) {
            posts.add(parseStatus(status));
        }

        return posts;
    }

    /**
     * 시작 게시글(주소를 붙여넣은 그 글) 자체의 JSON을
     * RpPost 하나로 변환합니다. context API의 descendants에는
     * 시작 글이 포함되지 않으므로 따로 파싱합니다.
     */
    public RpPost parseStartingPost(String statusJson)
            throws IOException {

        if (statusJson == null || statusJson.isBlank()) {
            throw new IllegalArgumentException(
                    "파싱할 JSON 문자열이 비어 있습니다."
            );
        }

        JsonNode status = objectMapper.readTree(statusJson);

        return parseStatus(status);
    }

    private RpPost parseStatus(JsonNode status) {
        String id =
                status.path("id").asText();

        String parentId =
                getNullableText(
                        status,
                        "in_reply_to_id"
                );

        JsonNode account =
                status.path("account");

        String accountId =
                account.path("id").asText();

        String displayName =
                account.path("display_name").asText();

        String contentHtml =
                status.path("content").asText();

        Document document =
                Jsoup.parseBodyFragment(contentHtml);

        document.select("a.mention").remove();

        String content =
                document.text();

        return new RpPost(
                id,
                parentId,
                accountId,
                displayName,
                content
        );
    }

    /**
     * 시작글 ID 아래로 연결된 답글을 순서대로 찾습니다.
     *
     * 일반적인 일대일 RP처럼 각 글에 다음 답글이 하나씩
     * 이어지는 경우를 기준으로 합니다.
     */
    public List<RpPost> sortConversation(
            List<RpPost> posts,
            String startStatusId
    ) {
        Map<String, List<RpPost>> repliesByParent =
                new HashMap<>();

        for (RpPost post : posts) {
            repliesByParent
                    .computeIfAbsent(
                            post.parentId(),
                            key -> new ArrayList<>()
                    )
                    .add(post);
        }

        List<RpPost> orderedPosts =
                new ArrayList<>();

        Set<String> visitedIds =
                new HashSet<>();

        String currentParentId =
                startStatusId;

        while (true) {
            List<RpPost> replies =
                    repliesByParent.get(currentParentId);

            if (replies == null || replies.isEmpty()) {
                break;
            }

            /*
             * 일대일 RP라면 보통 답글이 하나뿐입니다.
             * 여러 개면 현재는 첫 번째 답글을 선택합니다.
             */
            RpPost nextPost =
                    replies.get(0);

            if (!visitedIds.add(nextPost.id())) {
                break;
            }

            orderedPosts.add(nextPost);
            currentParentId = nextPost.id();
        }

        return orderedPosts;
    }


    /**
     * 시작글의 직계 답글 중 지정한 캐릭터 이름과 일치하는 글을 찾아
     * 그 답글부터 이어지는 대화 분기를 순서대로 반환합니다.
     *
     * characterName이 비어 있으면 기존 방식대로 첫 번째 답글 분기를 따릅니다.
     * 이름 비교는 앞뒤 공백을 제거하고 대소문자를 구분하지 않습니다.
     */
    public List<RpPost> sortConversationByCharacter(
            List<RpPost> posts,
            String startStatusId,
            String characterName
    ) {
        if (characterName == null || characterName.isBlank()) {
            return sortConversation(posts, startStatusId);
        }

        String targetName = characterName.trim();

        Map<String, List<RpPost>> repliesByParent =
                new HashMap<>();

        for (RpPost post : posts) {
            repliesByParent
                    .computeIfAbsent(
                            post.parentId(),
                            key -> new ArrayList<>()
                    )
                    .add(post);
        }

        List<RpPost> firstReplies =
                repliesByParent.get(startStatusId);

        if (firstReplies == null || firstReplies.isEmpty()) {
            return new ArrayList<>();
        }

        RpPost firstMatch = null;

        for (RpPost reply : firstReplies) {
            String displayName = reply.displayName();

            if (displayName != null
                    && displayName.trim().equalsIgnoreCase(targetName)) {
                firstMatch = reply;
                break;
            }
        }

        if (firstMatch == null) {
            return new ArrayList<>();
        }

        List<RpPost> orderedPosts =
                new ArrayList<>();

        Set<String> visitedIds =
                new HashSet<>();

        RpPost currentPost = firstMatch;

        while (currentPost != null) {
            if (!visitedIds.add(currentPost.id())) {
                break;
            }

            orderedPosts.add(currentPost);

            List<RpPost> replies =
                    repliesByParent.get(currentPost.id());

            if (replies == null || replies.isEmpty()) {
                break;
            }

            /*
             * 선택한 분기 안에서는 기존 RP 구조와 마찬가지로
             * 첫 번째 답글을 다음 글로 간주합니다.
             */
            currentPost = replies.get(0);
        }

        return orderedPosts;
    }

    /**
     * 지정한 두 계정의 대화만 콘솔에 출력합니다.
     */
    public void printConversation(
            List<RpPost> posts,
            String firstAccountId,
            String secondAccountId
    ) {
        System.out.println();
        System.out.println(
                "========================================"
        );
        System.out.println(
                "              RP 대화"
        );
        System.out.println(
                "========================================"
        );

        int printedCount = 0;

        for (RpPost post : posts) {
            boolean isFirstCharacter =
                    post.accountId()
                            .equals(firstAccountId);

            boolean isSecondCharacter =
                    post.accountId()
                            .equals(secondAccountId);

            if (!isFirstCharacter
                    && !isSecondCharacter) {
                continue;
            }

            printedCount++;

            System.out.println();
            System.out.println(
                    "[" + post.displayName() + "]"
            );

            System.out.println(post.content());
        }

        if (printedCount == 0) {
            System.out.println();
            System.out.println(
                    "해당 계정들의 게시글을 찾지 못했습니다."
            );
        }

        System.out.println();
        System.out.println(
                "총 " + printedCount + "개 게시글"
        );
    }

    private String getNullableText(
            JsonNode node,
            String fieldName
    ) {
        JsonNode value =
                node.get(fieldName);

        if (value == null || value.isNull()) {
            return null;
        }

        return value.asText();
    }
}