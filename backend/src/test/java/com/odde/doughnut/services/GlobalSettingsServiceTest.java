package com.odde.doughnut.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
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
