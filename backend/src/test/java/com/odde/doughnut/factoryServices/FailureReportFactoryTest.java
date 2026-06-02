package com.odde.doughnut.factoryServices;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.FailureReportRepository;
import com.odde.doughnut.services.GithubService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;

class FailureReportFactoryTest {

  @Test
  void failureReportIncludesAuthenticatedUserInfo() throws IOException, InterruptedException {
    HttpServletRequest request = new MockHttpServletRequest();
    User user = new User();
    user.setExternalIdentifier("ext-test-user");
    user.setName("Test User");
    CurrentUserFetcher fetcher = mock(CurrentUserFetcher.class);
    when(fetcher.getExternalIdentifier()).thenReturn(user.getExternalIdentifier());
    when(fetcher.getUser()).thenReturn(user);
    FailureReportRepository repository = mock(FailureReportRepository.class);
    when(repository.save(any(FailureReport.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    GithubService githubService = mock(GithubService.class);
    doReturn(null).when(githubService).createGithubIssue(any());

    FailureReportFactory factory =
        new FailureReportFactory(
            request, new RuntimeException("boom"), fetcher, githubService, repository);
    factory.createUnlessAllowed();

    ArgumentCaptor<FailureReport> captor = ArgumentCaptor.forClass(FailureReport.class);
    verify(repository, atLeastOnce()).save(captor.capture());
    String errorDetail = captor.getAllValues().getFirst().getErrorDetail();
    assertThat(errorDetail, containsString(user.getExternalIdentifier()));
    assertThat(errorDetail, containsString(user.getName()));
  }
}
