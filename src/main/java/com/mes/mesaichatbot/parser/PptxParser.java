package com.mes.mesaichatbot.parser;

import com.mes.mesaichatbot.dto.UploadDataDetailsDto;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PptxParser implements DocumentParser {
    private final int companyId;
    private final String fileId;
    private final LocalDateTime createdAt;

    public PptxParser(int companyId, String fileId, LocalDateTime createdAt) {
        this.companyId = companyId;
        this.fileId = fileId;
        this.createdAt = createdAt;
    }

    @Override
    public boolean supports(String filename) {
        if (filename == null || filename.isBlank()) {
            return false;
        }

        return Objects.equals(StringUtils.getFilenameExtension(filename), "pptx");
    }

    @Override
    public List<UploadDataDetailsDto> parse(InputStream inputStream) throws IOException {
        List<UploadDataDetailsDto> data = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();

        try (XMLSlideShow presentation = new XMLSlideShow(inputStream)) {
            for (int i = 0; i < presentation.getSlides().size(); i++) {
                XSLFSlide slide = presentation.getSlides().get(i);
                data.add(new UploadDataDetailsDto(1, companyId, fileId, i + 1, mapper.writeValueAsString(slide), createdAt));
            }
            return data;
        }
    }
}
