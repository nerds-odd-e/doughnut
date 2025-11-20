package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.testability.MakeMe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class ControllerTestBase {
  @Autowired protected MakeMe makeMe;
  @Autowired protected AuthorizationService authorizationService;

  @TestBean protected CurrentUser currentUser;

  static CurrentUser currentUser() {
    return new CurrentUser();
  }
}
