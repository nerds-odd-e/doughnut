package com.odde.doughnut.services;

import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
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
}
