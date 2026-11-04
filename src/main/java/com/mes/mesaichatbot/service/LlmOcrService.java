package com.mes.mesaichatbot.service;

import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

@Service
@RequiredArgsConstructor
public class LlmOcrService {
    private final ObjectProvider<ChatClient.Builder> chatClientProvider;

    /**
     * ChatClient를 얻습니다.
     *
     * @return {@link ChatClient}
     * @throws UnsupportedOperationException ChatClient를 사용할 수 없을 때 발생
     */
    private ChatClient getChatClient() throws UnsupportedOperationException {
        ChatClient.Builder builder = chatClientProvider.getIfAvailable();
        if (builder == null) {
            throw new UnsupportedOperationException("ChatClient를 사용할 수 없습니다.");
        }
        return builder.build();
    }

    public String recognizeImage(XWPFPictureData image) {
        ChatClient chatClient = getChatClient();

        String systemPrompt = """
                당신은 OCR 전문가입니다.
                사용자가 첨부한 이미지를 한국어 문장으로 묘사하십시오.
                
                최대한 자세히 묘사할수록 좋습니다.
                단, 가능하다면 5줄 내외여야 합니다.
                """;
        return chatClient.prompt().system(systemPrompt).user(u -> u.text("여기 이미지입니다.").media(MimeType.valueOf(
                image.getPictureTypeEnum().contentType), new ByteArrayResource(image.getData()))).call().content();
    }
}
