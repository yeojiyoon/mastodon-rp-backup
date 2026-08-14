package com.jiyoon.mastodonrpbackup;

public record RpPost(
        String id,
        String parentId,
        String accountId,
        String displayName,
        String content
) {
}