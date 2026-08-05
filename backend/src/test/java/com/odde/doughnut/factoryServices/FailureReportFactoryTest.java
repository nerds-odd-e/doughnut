package com.odde.doughnut.factoryServices;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcherFromRequest;
import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.FailureReportRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
import com.odde.doughnut.services.GithubService;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.testability.MakeMe;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@ExtendWith(MockitoExtension.class)
class FailureReportFactoryTest {

  @Autowired FailureReportRepository failureReportRepository;
  @Autowired UserRepository userRepository;
  @Autowired UserService userService;
  @Autowired MakeMe makeMe;
  @Mock GithubService githubService;

  MockHttpServletRequest request = new MockHttpServletRequest();

  @BeforeEach
  void setUp() throws IOException, InterruptedException {
    doReturn(null).when(githubService).createGithubIssue(any());
  }

  @Test
  void recordsFailureReportForUnauthenticatedRequest() throws IOException, InterruptedException {
    FailureReport report = createReport();

    assertEquals("java.lang.RuntimeException", report.getErrorName());
    assertThat(report.getErrorDetail(), containsString("user external Id: null"));
    assertThat(report.getErrorDetail(), containsString("FailureReportFactoryTest.java"));
  }

  @Test
  void includesAuthenticatedUserInFailureReport() throws IOException, InterruptedException {
    User user = makeMe.aUser().please();
    request.setUserPrincipal(() -> user.getExternalIdentifier());

    FailureReport report = createReport();

    assertThat(report.getErrorDetail(), containsString(user.getExternalIdentifier()));
    assertThat(report.getErrorDetail(), containsString(user.getName()));
  }

  private FailureReport createReport() throws IOException, InterruptedException {
    CurrentUserFetcherFromRequest fetcher =
        new CurrentUserFetcherFromRequest(request, userRepository, userService, Optional.empty());

    new FailureReportFactory(
            request, new RuntimeException(), fetcher, githubService, failureReportRepository)
        .createUnlessAllowed();

    return failureReportRepository.findAll().iterator().next();
  }
}
