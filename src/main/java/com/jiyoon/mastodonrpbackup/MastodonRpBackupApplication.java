package com.jiyoon.mastodonrpbackup;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class MastodonRpBackupApplication extends Application {

    private final OAuthService oauthService =
            new OAuthService();

    private Stage stage;

    private OAuthService.OAuthApplication oauthApplication;
    private OAuthService.OAuthToken oauthToken;
    private String normalizedInstanceUrl;

    private final TextField instanceUrlField =
            new TextField("https://planet.moe");

    private final TextField authorizationCodeField =
            new TextField();

    private final TextField postUrlField =
            new TextField();

    private final TextField characterNameField =
            new TextField();

    private final TextField titleField =
            new TextField("rp-script");

    private final TextField savePathField =
            new TextField();

    private final Label loginStatusLabel =
            new Label("서버 주소를 입력하고 로그인하세요.");

    private final Label backupStatusLabel =
            new Label("백업 설정을 입력하세요.");

    private final Button openLoginPageButton =
            new Button("로그인 페이지 열기");

    private final Button completeLoginButton =
            new Button("로그인");

    private final Button browseButton =
            new Button("폴더 선택");

    private final Button saveButton =
            new Button("DOCX 저장");

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        savePathField.setText(
                System.getProperty("user.home")
        );

        savePathField.setEditable(false);

        stage.setTitle("Mastodon Backup");
        stage.setMinWidth(900);
        stage.setMinHeight(620);

        openLoginPageButton.setOnAction(
                event -> startOAuth()
        );

        completeLoginButton.setOnAction(
                event -> completeOAuthLogin()
        );

        browseButton.setOnAction(
                event -> chooseSaveDirectory()
        );

        saveButton.setOnAction(
                event -> saveDocument()
        );

        showLoginView();

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    /*
     * =========================================================
     * LOGIN VIEW
     * =========================================================
     */
    private void showLoginView() {
        stage.setTitle(
                "Mastodon Backup · Login"
        );

        VBox brandPanel =
                createBrandPanel();

        VBox loginCard =
                new VBox(18);

        loginCard.getStyleClass()
                .add("card");

        loginCard.setMaxWidth(470);

        Label eyebrow =
                new Label("ACCOUNT");

        eyebrow.getStyleClass()
                .add("eyebrow");

        Label title =
                new Label("마스토돈에 로그인");

        title.getStyleClass()
                .add("page-title");

        Label subtitle =
                new Label(
                        "로그인 후 백업 설정으로 이동합니다."
                );

        subtitle.getStyleClass()
                .add("muted");

        VBox serverBlock =
                fieldBlock(
                        "마스토돈 서버 주소",
                        "예: https://planet.moe",
                        instanceUrlField
                );

        authorizationCodeField.setPromptText(
                "브라우저에서 발급된 인증 코드"
        );

        VBox codeBlock =
                fieldBlock(
                        "인증 코드",
                        "승인 후 표시되는 코드를 붙여넣으세요.",
                        authorizationCodeField
                );

        openLoginPageButton.getStyleClass()
                .addAll(
                        "button-secondary",
                        "wide-button"
                );

        completeLoginButton.getStyleClass()
                .addAll(
                        "button-primary",
                        "wide-button"
                );

        HBox actionRow =
                new HBox(
                        10,
                        openLoginPageButton,
                        completeLoginButton
                );

        HBox.setHgrow(
                openLoginPageButton,
                Priority.ALWAYS
        );

        HBox.setHgrow(
                completeLoginButton,
                Priority.ALWAYS
        );

        openLoginPageButton.setMaxWidth(
                Double.MAX_VALUE
        );

        completeLoginButton.setMaxWidth(
                Double.MAX_VALUE
        );

        loginStatusLabel.getStyleClass()
                .setAll(
                        "status-label",
                        "muted"
                );

        loginCard.getChildren()
                .addAll(
                        eyebrow,
                        title,
                        subtitle,
                        spacer(4),
                        serverBlock,
                        codeBlock,
                        actionRow,
                        loginStatusLabel
                );

        HBox main =
                new HBox(
                        54,
                        brandPanel,
                        loginCard
                );

        main.setAlignment(
                Pos.CENTER
        );

        HBox.setHgrow(
                brandPanel,
                Priority.ALWAYS
        );

        Pane shell =
                createShell(main);

        setScene(shell);
    }

    private VBox createBrandPanel() {
        VBox brand =
                new VBox(18);

        brand.setMaxWidth(430);
        brand.setAlignment(
                Pos.CENTER_LEFT
        );

        Label product =
                new Label("MASTODON BACKUP");

        product.getStyleClass()
                .add("brand-pill");

        Label headline =
                new Label(
                        "Sign in to Mastodon"
                );

        headline.getStyleClass()
                .add("hero-title");

        Label description =
                new Label(
                        " "
                );

        description.getStyleClass()
                .add("hero-copy");

        VBox featureList =
                new VBox(
                        10,
                        feature("DOCX 자동 정리"),
                        feature("로컬 폴더 저장")
                );

        featureList.getStyleClass()
                .add("feature-list");

        brand.getChildren()
                .addAll(
                        product,
                        headline,
                        description,
                        featureList
                );

        return brand;
    }

    /*
     * =========================================================
     * BACKUP CONFIG VIEW
     * =========================================================
     */
    private void showBackupConfigView(
            String displayName
    ) {
        stage.setTitle(
                "Mastodon Backup · Config"
        );

        BorderPane page =
                new BorderPane();

        page.getStyleClass()
                .add("page");

        HBox header =
                createConfigHeader(
                        displayName
                );

        page.setTop(header);

        VBox content =
                new VBox(22);

        content.setMaxWidth(860);

        Label eyebrow =
                new Label("BACKUP CONFIG");

        eyebrow.getStyleClass()
                .add("eyebrow");

        Label title =
                new Label("백업 설정");

        title.getStyleClass()
                .add("page-title");

        Label subtitle =
                new Label(
                        "게시글과 캐릭터를 지정하세요."
                );

        subtitle.getStyleClass()
                .add("muted");

        VBox configCard =
                new VBox(20);

        configCard.getStyleClass()
                .add("card");

        postUrlField.setPromptText(
                "https://planet.moe/@account/123456..."
        );

        characterNameField.setPromptText(
                "마스토돈 표기 이름 (풀네임)"
        );

        titleField.setPromptText(
                "문서 파일명"
        );

        VBox postBlock =
                fieldBlock(
                        "시작 게시글 URL",
                        "RP가 시작되는 첫 게시글 주소",
                        postUrlField
                );

        VBox characterBlock =
                fieldBlock(
                        "캐릭터 이름",
                        "일치하는 첫 답글부터 백업합니다. 비워두면 첫 분기를 사용합니다.",
                        characterNameField
                );

        VBox titleBlock =
                fieldBlock(
                        "문서 제목",
                        "저장될 DOCX 파일명",
                        titleField
                );

        VBox pathBlock =
                createPathBlock();

        configCard.getChildren()
                .addAll(
                        postBlock,
                        characterBlock,
                        titleBlock,
                        pathBlock
                );

        Region grow =
                new Region();

        HBox.setHgrow(
                grow,
                Priority.ALWAYS
        );

        Button switchAccountButton =
                new Button("계정 전환");

        switchAccountButton.getStyleClass()
                .add("button-ghost");

        switchAccountButton.setOnAction(
                event -> logout()
        );

        saveButton.getStyleClass()
                .addAll(
                        "button-primary"
                );

        saveButton.setMinWidth(150);

        backupStatusLabel.getStyleClass()
                .setAll(
                        "status-label",
                        "muted"
                );

        HBox footer =
                new HBox(
                        12,
                        backupStatusLabel,
                        grow,
                        switchAccountButton,
                        saveButton
                );

        footer.setAlignment(
                Pos.CENTER_LEFT
        );

        content.getChildren()
                .addAll(
                        eyebrow,
                        title,
                        subtitle,
                        configCard,
                        footer
                );

        StackPane center =
                new StackPane(content);

        center.setPadding(
                new Insets(
                        34,
                        42,
                        42,
                        42
                )
        );

        page.setCenter(center);

        Pane shell =
                createShell(page);

        setScene(shell);
    }

    private HBox createConfigHeader(
            String displayName
    ) {
        Label logo =
                new Label(
                        "Mastodon Backup"
                );

        logo.getStyleClass()
                .add("header-logo");

        Region grow =
                new Region();

        HBox.setHgrow(
                grow,
                Priority.ALWAYS
        );

        Region dot =
                new Region();

        dot.getStyleClass()
                .add("online-dot");

        Label account =
                new Label(
                        displayName
                );

        account.getStyleClass()
                .add("account-label");

        HBox chip =
                new HBox(
                        8,
                        dot,
                        account
                );

        chip.setAlignment(
                Pos.CENTER_LEFT
        );

        chip.getStyleClass()
                .add("account-chip");

        HBox header =
                new HBox(
                        20,
                        logo,
                        grow,
                        chip
                );

        header.setAlignment(
                Pos.CENTER_LEFT
        );

        header.setPadding(
                new Insets(
                        20,
                        34,
                        20,
                        34
                )
        );

        header.getStyleClass()
                .add("top-bar");

        return header;
    }

    private VBox createPathBlock() {
        Label label =
                new Label("저장 경로");

        label.getStyleClass()
                .add("field-label");

        Label help =
                new Label(
                        "저장할 로컬 폴더"
                );

        help.getStyleClass()
                .add("field-help");

        savePathField.getStyleClass()
                .add("path-field");

        browseButton.getStyleClass()
                .addAll(
                        "button-secondary"
                );

        HBox row =
                new HBox(
                        10,
                        savePathField,
                        browseButton
                );

        HBox.setHgrow(
                savePathField,
                Priority.ALWAYS
        );

        VBox block =
                new VBox(
                        7,
                        label,
                        help,
                        row
                );

        return block;
    }

    /*
     * =========================================================
     * OAUTH
     * =========================================================
     */
    private void startOAuth() {
        String rawInstance =
                instanceUrlField
                        .getText()
                        .trim();

        if (rawInstance.isBlank()) {
            showWarning(
                    "마스토돈 서버 주소를 입력하세요."
            );
            return;
        }

        setLoginBusy(true);

        setLoginStatus(
                "로그인 페이지를 준비하고 있습니다..."
        );

        Task<OAuthStartResult> task =
                new Task<>() {
                    @Override
                    protected OAuthStartResult call()
                            throws Exception {

                        String instanceUrl =
                                oauthService
                                        .normalizeInstanceUrl(
                                                rawInstance
                                        );

                        OAuthService.OAuthApplication application =
                                oauthService
                                        .registerApplication(
                                                instanceUrl
                                        );

                        String authorizationUrl =
                                oauthService
                                        .createAuthorizationUrl(
                                                instanceUrl,
                                                application
                                        );

                        boolean browserOpened =
                                oauthService
                                        .openBrowser(
                                                authorizationUrl
                                        );

                        return new OAuthStartResult(
                                instanceUrl,
                                application,
                                authorizationUrl,
                                browserOpened
                        );
                    }
                };

        task.setOnSucceeded(
                event -> {
                    OAuthStartResult result =
                            task.getValue();

                    normalizedInstanceUrl =
                            result.instanceUrl();

                    oauthApplication =
                            result.application();

                    oauthToken = null;

                    if (result.browserOpened()) {
                        setLoginStatus(
                                "브라우저에서 로그인한 뒤 인증 코드를 입력하세요."
                        );
                    } else {
                        javafx.scene.input.Clipboard clipboard =
                                javafx.scene.input.Clipboard
                                        .getSystemClipboard();

                        javafx.scene.input.ClipboardContent content =
                                new javafx.scene.input.ClipboardContent();

                        content.putString(
                                result.authorizationUrl()
                        );

                        clipboard.setContent(
                                content
                        );

                        setLoginStatus(
                                "로그인 주소를 클립보드에 복사했습니다."
                        );
                    }

                    authorizationCodeField
                            .requestFocus();

                    setLoginBusy(false);
                }
        );

        task.setOnFailed(
                event -> {
                    setLoginBusy(false);
                    showException(
                            task.getException(),
                            loginStatusLabel
                    );
                }
        );

        startTask(task);
    }

    private void completeOAuthLogin() {
        if (oauthApplication == null
                || normalizedInstanceUrl == null) {

            showWarning(
                    "먼저 '로그인 페이지 열기'를 눌러주세요."
            );
            return;
        }

        String authorizationCode =
                authorizationCodeField
                        .getText()
                        .trim();

        if (authorizationCode.isBlank()) {
            showWarning(
                    "인증 코드를 입력하세요."
            );
            return;
        }

        setLoginBusy(true);

        setLoginStatus(
                "계정을 확인하고 있습니다..."
        );

        Task<LoginResult> task =
                new Task<>() {
                    @Override
                    protected LoginResult call()
                            throws Exception {

                        OAuthService.OAuthToken token =
                                oauthService
                                        .exchangeCodeForToken(
                                                normalizedInstanceUrl,
                                                oauthApplication,
                                                authorizationCode
                                        );

                        String displayName =
                                oauthService
                                        .getCurrentAccountDisplayName(
                                                normalizedInstanceUrl,
                                                token.accessToken()
                                        );

                        return new LoginResult(
                                token,
                                displayName
                        );
                    }
                };

        task.setOnSucceeded(
                event -> {
                    LoginResult result =
                            task.getValue();

                    oauthToken =
                            result.token();

                    authorizationCodeField
                            .clear();

                    setLoginBusy(false);

                    showBackupConfigView(
                            result.displayName()
                    );
                }
        );

        task.setOnFailed(
                event -> {
                    oauthToken = null;
                    setLoginBusy(false);
                    showException(
                            task.getException(),
                            loginStatusLabel
                    );
                }
        );

        startTask(task);
    }

    /*
     * =========================================================
     * BACKUP
     * =========================================================
     */
    private void saveDocument() {
        if (oauthToken == null) {
            showWarning(
                    "로그인이 필요합니다."
            );
            showLoginView();
            return;
        }

        String postUrl =
                postUrlField
                        .getText()
                        .trim();

        String characterName =
                characterNameField
                        .getText()
                        .trim();

        String title =
                titleField
                        .getText()
                        .trim();

        String savePath =
                savePathField
                        .getText()
                        .trim();

        if (postUrl.isBlank()) {
            showWarning(
                    "시작 게시글 URL을 입력하세요."
            );
            postUrlField.requestFocus();
            return;
        }

        if (title.isBlank()) {
            showWarning(
                    "문서 제목을 입력하세요."
            );
            titleField.requestFocus();
            return;
        }

        if (savePath.isBlank()) {
            showWarning(
                    "저장 폴더를 선택하세요."
            );
            return;
        }

        setBackupBusy(true);

        setBackupStatus(
                "타래를 읽고 DOCX를 생성하고 있습니다..."
        );

        Task<Path> task =
                new Task<>() {
                    @Override
                    protected Path call()
                            throws Exception {

                        MastodonPostCall postCall =
                                new MastodonPostCall();

                        String contextJson =
                                postCall.getContext(
                                        postUrl,
                                        oauthToken.accessToken()
                                );

                        String startStatusId =
                                extractStatusId(
                                        postUrl
                                );

                        MastodonContextParser parser =
                                new MastodonContextParser();

                        List<RpPost> parsedPosts =
                                parser.parse(
                                        contextJson
                                );

                        List<RpPost> orderedPosts =
                                parser.sortConversationByCharacter(
                                        parsedPosts,
                                        startStatusId,
                                        characterName
                                );

                        if (!characterName.isBlank()
                                && orderedPosts.isEmpty()) {

                            throw new IllegalArgumentException(
                                    "시작 게시글의 첫 답글에서 캐릭터 이름 '"
                                            + characterName
                                            + "'을 찾지 못했습니다."
                            );
                        }

                        String startStatusJson =
                                postCall.getStatus(
                                        postUrl,
                                        oauthToken.accessToken()
                                );

                        RpPost startingPost =
                                parser.parseStartingPost(
                                        startStatusJson
                                );

                        List<RpPost> postsWithStart =
                                new java.util.ArrayList<>();

                        postsWithStart.add(startingPost);
                        postsWithStart.addAll(orderedPosts);

                        RpDocxWriter writer =
                                new RpDocxWriter();

                        return writer.writeUnique(
                                postsWithStart,
                                Path.of(savePath),
                                title
                        );
                    }
                };

        task.setOnSucceeded(
                event -> {
                    Path savedPath =
                            task.getValue();

                    setBackupBusy(false);

                    setBackupStatus(
                            "저장 완료 · "
                                    + savedPath
                                    .getFileName()
                    );

                    Alert alert =
                            new Alert(
                                    Alert.AlertType.INFORMATION
                            );

                    alert.setTitle(
                            "저장 완료"
                    );

                    alert.setHeaderText(
                            "DOCX 백업이 완료되었습니다."
                    );

                    alert.setContentText(
                            savedPath.toString()
                    );

                    alert.showAndWait();
                }
        );

        task.setOnFailed(
                event -> {
                    setBackupBusy(false);

                    showException(
                            task.getException(),
                            backupStatusLabel
                    );
                }
        );

        startTask(task);
    }

    private void chooseSaveDirectory() {
        DirectoryChooser chooser =
                new DirectoryChooser();

        chooser.setTitle(
                "DOCX 저장 폴더 선택"
        );

        String currentPath =
                savePathField
                        .getText()
                        .trim();

        if (!currentPath.isBlank()) {
            Path current =
                    Path.of(currentPath);

            if (Files.isDirectory(current)) {
                chooser.setInitialDirectory(
                        current.toFile()
                );
            }
        }

        File selected =
                chooser.showDialog(
                        stage
                );

        if (selected != null) {
            savePathField.setText(
                    selected.getAbsolutePath()
            );
        }
    }

    private void logout() {
        oauthToken = null;
        oauthApplication = null;
        normalizedInstanceUrl = null;

        authorizationCodeField.clear();

        loginStatusLabel.setText(
                "서버 주소를 입력하고 로그인을 시작하세요."
        );

        showLoginView();
    }

    /*
     * =========================================================
     * UI HELPERS
     * =========================================================
     */
    private Pane createShell(
            javafx.scene.Node content
    ) {
        StackPane centerPane =
                new StackPane(content);

        centerPane.setPadding(
                new Insets(34, 34, 12, 34)
        );

        HBox footer =
                createCreditFooter();

        footer.setPadding(
                new Insets(0, 34, 22, 34)
        );

        BorderPane shell =
                new BorderPane();

        shell.getStyleClass()
                .add("app-shell");

        shell.setCenter(centerPane);
        shell.setBottom(footer);

        return shell;
    }

    private HBox createCreditFooter() {
        HBox githubChip =
                creditChip("icon-github", "yeojiyoon");

        HBox discordChip =
                creditChip("icon-discord", "jy___02");

        Region grow =
                new Region();

        HBox.setHgrow(
                grow,
                Priority.ALWAYS
        );

        HBox footer =
                new HBox(
                        10,
                        grow,
                        githubChip,
                        discordChip
                );

        footer.setAlignment(
                Pos.CENTER_RIGHT
        );

        return footer;
    }

    private HBox creditChip(
            String iconStyleClass,
            String handle
    ) {
        Region icon =
                new Region();

        icon.getStyleClass()
                .add(iconStyleClass);

        Label handleLabel =
                new Label(handle);

        handleLabel.getStyleClass()
                .add("account-label");

        HBox chip =
                new HBox(
                        7,
                        icon,
                        handleLabel
                );

        chip.setAlignment(
                Pos.CENTER_LEFT
        );

        chip.getStyleClass()
                .add("account-chip");

        return chip;
    }

    private VBox fieldBlock(
            String labelText,
            String helpText,
            TextField field
    ) {
        Label label =
                new Label(labelText);

        label.getStyleClass()
                .add("field-label");

        Label help =
                new Label(helpText);

        help.setWrapText(true);

        help.getStyleClass()
                .add("field-help");

        VBox block =
                new VBox(
                        7,
                        label,
                        help,
                        field
                );

        return block;
    }

    private HBox feature(
            String text
    ) {
        Region dot =
                new Region();

        dot.getStyleClass()
                .add("feature-dot");

        Label label =
                new Label(text);

        label.getStyleClass()
                .add("feature-text");

        HBox row =
                new HBox(
                        10,
                        dot,
                        label
                );

        row.setAlignment(
                Pos.CENTER_LEFT
        );

        return row;
    }

    private Region spacer(
            double height
    ) {
        Region spacer =
                new Region();

        spacer.setMinHeight(height);

        return spacer;
    }

    private void setScene(
            Pane root
    ) {
        Scene scene =
                new Scene(
                        root,
                        1120,
                        720
                );

        scene.getStylesheets()
                .add(
                        getClass()
                                .getResource(
                                        "/mastodon-rp-backup.css"
                                )
                                .toExternalForm()
                );

        stage.setScene(scene);
    }

    private void setLoginBusy(
            boolean busy
    ) {
        openLoginPageButton.setDisable(busy);
        completeLoginButton.setDisable(busy);
        instanceUrlField.setDisable(busy);
        authorizationCodeField.setDisable(busy);
    }

    private void setBackupBusy(
            boolean busy
    ) {
        saveButton.setDisable(busy);
        browseButton.setDisable(busy);
        postUrlField.setDisable(busy);
        characterNameField.setDisable(busy);
        titleField.setDisable(busy);
    }

    private void setLoginStatus(
            String message
    ) {
        loginStatusLabel.setText(message);
    }

    private void setBackupStatus(
            String message
    ) {
        backupStatusLabel.setText(message);
    }

    private void showWarning(
            String message
    ) {
        Alert alert =
                new Alert(
                        Alert.AlertType.WARNING
                );

        alert.setTitle(
                "입력 확인"
        );

        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showException(
            Throwable throwable,
            Label statusLabel
    ) {
        Throwable cause =
                throwable;

        while (cause != null
                && cause.getCause() != null) {
            cause =
                    cause.getCause();
        }

        String message =
                cause == null
                        ? "알 수 없는 오류가 발생했습니다."
                        : cause.getMessage();

        if (message == null
                || message.isBlank()) {
            message =
                    cause.getClass()
                            .getSimpleName();
        }

        statusLabel.setText(
                "오류 · "
                        + message
        );

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR
                );

        alert.setTitle(
                "오류"
        );

        alert.setHeaderText(
                "작업을 완료하지 못했습니다."
        );

        alert.setContentText(message);
        alert.showAndWait();
    }

    private void startTask(
            Task<?> task
    ) {
        Thread thread =
                new Thread(task);

        thread.setDaemon(true);
        thread.start();
    }

    private String extractStatusId(
            String postUrl
    ) {
        String trimmed =
                postUrl.trim();

        while (trimmed.endsWith("/")) {
            trimmed =
                    trimmed.substring(
                            0,
                            trimmed.length() - 1
                    );
        }

        int lastSlash =
                trimmed.lastIndexOf('/');

        if (lastSlash < 0
                || lastSlash
                == trimmed.length() - 1) {

            throw new IllegalArgumentException(
                    "시작 게시글 URL에서 게시글 ID를 찾을 수 없습니다."
            );
        }

        return trimmed.substring(
                lastSlash + 1
        );
    }

    private record OAuthStartResult(
            String instanceUrl,
            OAuthService.OAuthApplication application,
            String authorizationUrl,
            boolean browserOpened
    ) {
    }

    private record LoginResult(
            OAuthService.OAuthToken token,
            String displayName
    ) {
    }
}