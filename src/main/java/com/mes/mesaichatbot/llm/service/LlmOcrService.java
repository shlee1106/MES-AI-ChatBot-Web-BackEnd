package com.mes.mesaichatbot.llm.service;

import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

@Service
@ConditionalOnBean({ChatClient.class, VectorStore.class})
public class LlmOcrService {
    private ChatClient chatClient;

    @Autowired
    public void setChatClient(ChatClient.Builder chatClient) {
        this.chatClient = chatClient.build();
    }

    public String recognizeImage(XWPFPictureData image) {
        String systemPrompt = """
                당신은 OCR 전문가입니다.
                사용자가 첨부한 이미지를 한국어 문장으로 묘사하십시오.
                
                최대한 자세히 묘사할수록 좋습니다.
                단, 가능하다면 5줄 내외여야 합니다.
                """;
        return chatClient.prompt().system(systemPrompt).user(u -> u.text("여기 이미지입니다.").media(MimeType.valueOf(image.getPictureTypeEnum().contentType), new ByteArrayResource(image.getData()))).call().content();
    }
}
