package com.jiyoon.mastodonrpbackup;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;

public class MastodonRpBackupGui extends JFrame {

    private final JTextField instanceUrlField =
            new JTextField("https://planet.moe", 35);

    private final JTextField authorizationCodeField =
            new JTextField(35);

    private final JTextField postUrlField =
            new JTextField(35);

    private final JTextField characterNameField =
            new JTextField(35);

    private final JTextField titleField =
            new JTextField("rp-script", 35);

    private final JTextField savePathField =
            new JTextField(System.getProperty("user.home"), 35);

    private final JButton loginButton =
            new JButton("로그인 페이지 열기");

    private final JButton saveButton =
            new JButton("DOCX 저장");

    private final JLabel statusLabel =
            new JLabel("마스토돈 서버 주소를 입력하고 로그인을 시작하세요.");

    private final OAuthService oauthService =
            new OAuthService();

    private OAuthService.OAuthApplication oauthApplication;
    private String normalizedInstanceUrl;

    public MastodonRpBackupGui() {
        setTitle("Mastodon RP Backup");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(720, 480);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBorder(new EmptyBorder(20, 20, 20, 20));
        setContentPane(root);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addField(form, gbc, 0,
                "마스토돈 서버 주소", instanceUrlField);
        addField(form, gbc, 1,
                "인증 코드", authorizationCodeField);
        addField(form, gbc, 2,
                "시작 게시글 URL", postUrlField);
        addField(form, gbc, 3,
                "캐릭터 이름 (선택)", characterNameField);
        addField(form, gbc, 4,
                "문서 제목", titleField);
        addField(form, gbc, 5,
                "저장 경로", savePathField);

        root.add(form, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(loginButton);
        buttons.add(saveButton);

        statusLabel.setBorder(new EmptyBorder(8, 4, 8, 4));

        bottom.add(statusLabel);
        bottom.add(buttons);
        root.add(bottom, BorderLayout.SOUTH);

        loginButton.addActionListener(e -> startOAuth());
        saveButton.addActionListener(e -> saveDocument());
    }

    private void addField(
            JPanel panel,
            GridBagConstraints gbc,
            int row,
            String label,
            JTextField field
    ) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }

    private void startOAuth() {
        setBusy(true);
        setStatus("OAuth 애플리케이션을 등록하고 있습니다...");

        new SwingWorker<OAuthStartResult, Void>() {
            @Override
            protected OAuthStartResult doInBackground()
                    throws Exception {

                String instanceUrl =
                        oauthService.normalizeInstanceUrl(
                                instanceUrlField.getText()
                        );

                OAuthService.OAuthApplication application =
                        oauthService.registerApplication(instanceUrl);

                String authorizationUrl =
                        oauthService.createAuthorizationUrl(
                                instanceUrl,
                                application
                        );

                boolean browserOpened =
                        oauthService.openBrowser(authorizationUrl);

                return new OAuthStartResult(
                        instanceUrl,
                        application,
                        authorizationUrl,
                        browserOpened
                );
            }

            @Override
            protected void done() {
                try {
                    OAuthStartResult result = get();

                    normalizedInstanceUrl = result.instanceUrl();
                    oauthApplication = result.application();

                    if (result.browserOpened()) {
                        setStatus(
                                "브라우저에서 로그인 후 표시된 인증 코드를 입력하세요."
                        );
                    } else {
                        setStatus(
                                "브라우저를 자동으로 열지 못했습니다. 로그인 주소를 클립보드에 복사합니다."
                        );
                        copyToClipboard(result.authorizationUrl());
                    }

                } catch (Exception ex) {
                    showError(ex);
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void saveDocument() {
        if (oauthApplication == null
                || normalizedInstanceUrl == null) {
            showMessage("먼저 '로그인 페이지 열기'를 눌러 OAuth 로그인을 시작하세요.");
            return;
        }

        String authorizationCode =
                authorizationCodeField.getText().trim();
        String postUrl = postUrlField.getText().trim();
        String characterName = characterNameField.getText().trim();
        String title = titleField.getText().trim();
        String savePath = savePathField.getText().trim();

        if (authorizationCode.isBlank()) {
            showMessage("인증 코드를 입력하세요.");
            return;
        }

        if (postUrl.isBlank()) {
            showMessage("시작 게시글 URL을 입력하세요.");
            return;
        }

        if (title.isBlank()) {
            showMessage("문서 제목을 입력하세요.");
            return;
        }

        if (savePath.isBlank()) {
            showMessage("저장 경로를 입력하세요.");
            return;
        }

        setBusy(true);
        setStatus("게시글을 불러와 DOCX 문서를 생성하고 있습니다...");

        new SwingWorker<Path, Void>() {
            @Override
            protected Path doInBackground()
                    throws Exception {

                OAuthService.OAuthToken token =
                        oauthService.exchangeCodeForToken(
                                normalizedInstanceUrl,
                                oauthApplication,
                                authorizationCode
                        );

                MastodonPostCall postCall =
                        new MastodonPostCall();

                String contextJson =
                        postCall.getContext(
                                postUrl,
                                token.accessToken()
                        );

                String startStatusId =
                        extractStatusId(postUrl);

                MastodonContextParser contextParser =
                        new MastodonContextParser();

                List<RpPost> parsedPosts =
                        contextParser.parse(contextJson);

                List<RpPost> orderedPosts =
                        contextParser.sortConversationByCharacter(
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

                RpDocxWriter writer =
                        new RpDocxWriter();

                return writer.writeUnique(
                        orderedPosts,
                        Path.of(savePath),
                        title
                );
            }

            @Override
            protected void done() {
                try {
                    Path savedPath = get();
                    setStatus("저장 완료: " + savedPath);

                    JOptionPane.showMessageDialog(
                            MastodonRpBackupGui.this,
                            "문서 저장이 완료되었습니다.\n\n"
                                    + savedPath,
                            "저장 완료",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                } catch (Exception ex) {
                    showError(ex);
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private String extractStatusId(String postUrl) {
        String trimmed = postUrl.trim();

        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        int lastSlash = trimmed.lastIndexOf('/');

        if (lastSlash < 0 || lastSlash == trimmed.length() - 1) {
            throw new IllegalArgumentException(
                    "시작 게시글 URL에서 게시글 ID를 찾을 수 없습니다."
            );
        }

        return trimmed.substring(lastSlash + 1);
    }

    private void setBusy(boolean busy) {
        loginButton.setEnabled(!busy);
        saveButton.setEnabled(!busy);
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "입력 확인",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void showError(Exception exception) {
        Throwable cause = exception;

        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        String message = cause.getMessage();

        if (message == null || message.isBlank()) {
            message = cause.getClass().getSimpleName();
        }

        setStatus("오류: " + message);

        JOptionPane.showMessageDialog(
                this,
                message,
                "오류",
                JOptionPane.ERROR_MESSAGE
        );
    }

    private void copyToClipboard(String text) {
        try {
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(
                            new java.awt.datatransfer.StringSelection(text),
                            null
                    );
        } catch (IllegalStateException ignored) {
            // 클립보드를 사용할 수 없어도 프로그램 진행에는 문제가 없습니다.
        }
    }

    private record OAuthStartResult(
            String instanceUrl,
            OAuthService.OAuthApplication application,
            String authorizationUrl,
            boolean browserOpened
    ) {
    }
}