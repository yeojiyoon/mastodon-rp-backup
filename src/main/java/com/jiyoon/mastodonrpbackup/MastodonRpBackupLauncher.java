package com.jiyoon.mastodonrpbackup;

import javafx.application.Application;

/**
 * IntelliJ / classpath 실행용 진입점.
 *
 * 중요:
 * MastodonRpBackupApplication을 직접 실행하지 말고
 * 이 클래스를 실행해야 합니다.
 */
public class MastodonRpBackupLauncher {

    public static void main(String[] args) {
        Application.launch(
                MastodonRpBackupApplication.class,
                args
        );
    }
}