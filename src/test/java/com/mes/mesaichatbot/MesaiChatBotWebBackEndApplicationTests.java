package com.mes.mesaichatbot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MesaiChatBotWebBackEndApplicationTest {

    @Test
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
    void contextLoads() {
    }

}
