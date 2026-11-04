package com.mes.mesaichatbot;

import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class MesaiChatBotWebBackEndApplicationTest {

//    @Test
//    void contextLoads() {
//    }

}
