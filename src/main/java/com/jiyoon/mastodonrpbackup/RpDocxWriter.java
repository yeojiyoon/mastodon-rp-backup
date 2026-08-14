package com.jiyoon.mastodonrpbackup;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class RpDocxWriter {

    /*
     * DOCX 표 너비 단위는 twip입니다.
     *
     * 전체 9,000 중:
     * 이름 열 2,700
     * 대사 열 6,300
     */
    private static final int TABLE_WIDTH = 9000;
    private static final int NAME_COLUMN_WIDTH = 2700;
    private static final int CONTENT_COLUMN_WIDTH = 6300;


    /**
     * 사용자가 지정한 저장 폴더와 제목으로 DOCX 파일을 저장합니다.
     * 같은 이름의 파일이 이미 있으면 (1), (2) ... 를 붙입니다.
     *
     * 예:
     * RP.docx
     * RP (1).docx
     * RP (2).docx
     */
    public Path writeUnique(
            List<RpPost> posts,
            Path directory,
            String title
    ) throws IOException {

        if (directory == null) {
            throw new IllegalArgumentException(
                    "저장 경로가 비어 있습니다."
            );
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "문서 제목이 비어 있습니다."
            );
        }

        String normalizedTitle = title.trim();

        if (normalizedTitle.toLowerCase().endsWith(".docx")) {
            normalizedTitle = normalizedTitle.substring(
                    0,
                    normalizedTitle.length() - 5
            ).trim();
        }

        if (normalizedTitle.isBlank()) {
            throw new IllegalArgumentException(
                    "문서 제목이 비어 있습니다."
            );
        }

        if (containsInvalidFileNameCharacter(normalizedTitle)) {
            throw new IllegalArgumentException(
                    "문서 제목에는 \\ / : * ? \" < > | 문자를 사용할 수 없습니다."
            );
        }

        Files.createDirectories(directory);

        Path outputPath = resolveUniquePath(
                directory,
                normalizedTitle
        );

        write(posts, outputPath.toString());
        return outputPath;
    }

    private boolean containsInvalidFileNameCharacter(
            String title
    ) {
        String invalidCharacters = "\\/:*?\"<>|";

        for (int i = 0; i < title.length(); i++) {
            if (invalidCharacters.indexOf(title.charAt(i)) >= 0) {
                return true;
            }
        }

        return false;
    }

    private Path resolveUniquePath(
            Path directory,
            String title
    ) {
        Path candidate =
                directory.resolve(title + ".docx");

        int number = 1;

        while (Files.exists(candidate)) {
            candidate = directory.resolve(
                    title + " (" + number + ").docx"
            );
            number++;
        }

        return candidate;
    }

    public void write(
            List<RpPost> posts,
            String outputPath
    ) throws IOException {

        if (posts == null) {
            throw new IllegalArgumentException(
                    "출력할 RP 게시글 목록이 없습니다."
            );
        }

        if (outputPath == null || outputPath.isBlank()) {
            throw new IllegalArgumentException(
                    "출력 파일 경로가 비어 있습니다."
            );
        }

        try (XWPFDocument document = new XWPFDocument()) {

            /*
             * 제목을 나중에 추가할 수도 있도록
             * 첫 문단을 만들어 둡니다.
             */
            XWPFParagraph title =
                    document.createParagraph();

            title.setAlignment(
                    ParagraphAlignment.CENTER
            );

            title.setSpacingAfter(0);

            /*
             * 제목이 필요하다면 아래 주석을 해제하면 됩니다.
             *
             * XWPFRun titleRun = title.createRun();
             * titleRun.setText("RP SCRIPT");
             * titleRun.setBold(true);
             * titleRun.setFontSize(18);
             */

            if (posts.isEmpty()) {
                XWPFParagraph emptyMessage =
                        document.createParagraph();

                emptyMessage.createRun()
                        .setText("출력할 RP 대화가 없습니다.");

            } else {
                XWPFTable table =
                        document.createTable(
                                posts.size(),
                                2
                        );

                configureTable(table);
                removeTableBorders(table);

                for (int i = 0; i < posts.size(); i++) {
                    RpPost post = posts.get(i);
                    XWPFTableRow row = table.getRow(i);

                    XWPFTableCell nameCell =
                            row.getCell(0);

                    XWPFTableCell contentCell =
                            row.getCell(1);

                    configureRowCells(
                            nameCell,
                            contentCell
                    );

                    writeName(
                            nameCell,
                            post.displayName()
                    );

                    writeContent(
                            contentCell,
                            post.content()
                    );
                }
            }

            try (FileOutputStream outputStream =
                         new FileOutputStream(outputPath)) {

                document.write(outputStream);
            }
        }
    }

    /**
     * 표 전체 너비와 레이아웃 방식을 설정합니다.
     *
     * 자동 너비 계산을 막고 지정한 열 너비를
     * 최대한 유지하도록 fixed 레이아웃을 사용합니다.
     */
    private void configureTable(
            XWPFTable table
    ) {
        table.setWidth(TABLE_WIDTH);

        CTTblPr tableProperties =
                table.getCTTbl().getTblPr();

        if (tableProperties == null) {
            tableProperties =
                    table.getCTTbl().addNewTblPr();
        }

        if (tableProperties.getTblLayout() == null) {
            tableProperties.addNewTblLayout();
        }

        tableProperties
                .getTblLayout()
                .setType(STTblLayoutType.FIXED);

        /*
         * 표 내부 셀의 기본 여백을 조금 줄입니다.
         */
        var tableCellMargins =
                tableProperties.getTblCellMar();

        if (tableCellMargins == null) {
            tableCellMargins =
                    tableProperties.addNewTblCellMar();
        }

        setMarginWidth(
                tableCellMargins.addNewTop(),
                60
        );

        setMarginWidth(
                tableCellMargins.addNewBottom(),
                60
        );

        setMarginWidth(
                tableCellMargins.addNewLeft(),
                80
        );

        setMarginWidth(
                tableCellMargins.addNewRight(),
                80
        );
    }

    /**
     * 각 행의 이름 열과 대사 열 너비를 설정합니다.
     */
    private void configureRowCells(
            XWPFTableCell nameCell,
            XWPFTableCell contentCell
    ) {
        setCellWidth(
                nameCell,
                NAME_COLUMN_WIDTH
        );

        setCellWidth(
                contentCell,
                CONTENT_COLUMN_WIDTH
        );

        /*
         * 긴 이름이 자동으로 두 줄로 나뉘는 것을
         * 최대한 방지합니다.
         */
        setNoWrap(nameCell);
    }

    private void writeName(
            XWPFTableCell cell,
            String displayName
    ) {
        clearCell(cell);

        XWPFParagraph paragraph =
                cell.addParagraph();

        paragraph.setSpacingBefore(0);
        paragraph.setSpacingAfter(0);
        paragraph.setSpacingBetween(1.0);

        XWPFRun run = paragraph.createRun();

        run.setText(
                displayName == null
                        ? ""
                        : displayName
        );

        run.setBold(true);
        run.setFontSize(10);

        /*
         * 한글과 영문 글꼴을 모두 지정합니다.
         * 필요하면 "맑은 고딕"을 다른 글꼴로 바꿔도 됩니다.
         */
        run.setFontFamily("맑은 고딕");
    }

    private void writeContent(
            XWPFTableCell cell,
            String content
    ) {
        clearCell(cell);

        XWPFParagraph paragraph =
                cell.addParagraph();

        paragraph.setSpacingBefore(0);
        paragraph.setSpacingAfter(160);
        paragraph.setSpacingBetween(1.4);

        XWPFRun run = paragraph.createRun();

        run.setText(
                content == null
                        ? ""
                        : content
        );

        run.setFontSize(10);
        run.setFontFamily("맑은 고딕");
    }

    /**
     * 셀에 처음부터 존재하는 빈 문단을 모두 제거합니다.
     */
    private void clearCell(
            XWPFTableCell cell
    ) {
        while (!cell.getParagraphs().isEmpty()) {
            cell.removeParagraph(0);
        }
    }

    /**
     * 셀 너비를 twip 단위로 지정합니다.
     */
    private void setCellWidth(
            XWPFTableCell cell,
            int width
    ) {
        CTTcPr cellProperties =
                getOrCreateCellProperties(cell);

        var cellWidth =
                cellProperties.getTcW();

        if (cellWidth == null) {
            cellWidth =
                    cellProperties.addNewTcW();
        }

        cellWidth.setW(
                BigInteger.valueOf(width)
        );
    }

    /**
     * 이름 셀에서 자동 줄바꿈을 방지합니다.
     */
    private void setNoWrap(
            XWPFTableCell cell
    ) {
        CTTcPr cellProperties =
                getOrCreateCellProperties(cell);

        if (cellProperties.getNoWrap() == null) {
            cellProperties.addNewNoWrap();
        }
    }

    private CTTcPr getOrCreateCellProperties(
            XWPFTableCell cell
    ) {
        CTTcPr cellProperties =
                cell.getCTTc().getTcPr();

        if (cellProperties == null) {
            cellProperties =
                    cell.getCTTc().addNewTcPr();
        }

        return cellProperties;
    }

    private void setMarginWidth(
            org.openxmlformats.schemas
                    .wordprocessingml.x2006.main.CTTblWidth margin,
            int width
    ) {
        margin.setW(
                BigInteger.valueOf(width)
        );
    }

    private void removeTableBorders(
            XWPFTable table
    ) {
        CTTblPr tableProperties =
                table.getCTTbl().getTblPr();

        if (tableProperties == null) {
            tableProperties =
                    table.getCTTbl().addNewTblPr();
        }

        var borders =
                tableProperties.getTblBorders();

        if (borders == null) {
            borders =
                    tableProperties.addNewTblBorders();
        }

        if (borders.getTop() == null) {
            borders.addNewTop();
        }

        if (borders.getBottom() == null) {
            borders.addNewBottom();
        }

        if (borders.getLeft() == null) {
            borders.addNewLeft();
        }

        if (borders.getRight() == null) {
            borders.addNewRight();
        }

        if (borders.getInsideH() == null) {
            borders.addNewInsideH();
        }

        if (borders.getInsideV() == null) {
            borders.addNewInsideV();
        }

        borders.getTop().setVal(STBorder.NONE);
        borders.getBottom().setVal(STBorder.NONE);
        borders.getLeft().setVal(STBorder.NONE);
        borders.getRight().setVal(STBorder.NONE);
        borders.getInsideH().setVal(STBorder.NONE);
        borders.getInsideV().setVal(STBorder.NONE);
    }

}
