package com.odde.doughnut.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GlobalSettingsServiceTest {
  @Autowired MakeMe makeMe;
  GlobalSettingsService globalSettingsService;

  @BeforeEach
  void Setup() {
    globalSettingsService = new GlobalSettingsService(makeMe.modelFactoryService);
  }

  @Test
  void ShouldGetDefaultAssistantID() {
    GlobalSettingsService.GlobalSettingsKeyValue noteCompletionAssistantId =
        globalSettingsService.getNoteCompletionAssistantId();
    assertThat(noteCompletionAssistantId.getValue()).startsWith("asst_");
  }
}
