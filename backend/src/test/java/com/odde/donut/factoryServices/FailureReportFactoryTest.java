package com.odde.donut.factoryServices;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.odde.donut.controllers.currentUser.CurrentUserFetcherFromRequest;
import com.odde.donut.entities.FailureReport;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.FailureReportRepository;
import com.odde.donut.entities.repositories.UserRepository;
import com.odde.donut.services.GithubService;
import com.odde.donut.services.UserService;
import com.odde.donut.testability.MakeMe;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
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

  @Test
  void recordsFailureReportFromExceptionAndSourceWithoutHttpRequest() {
    FailureReport report = createReportFromException();
    assertThat(
        report.getErrorDetail(), containsString("# source: QuestionGenerationBatchMaintenanceJob"));
    assertThat(report.getErrorDetail(), not(containsString("# request:")));
    assertThat(report.getErrorDetail(), not(containsString("# user external Id:")));
  }

  @Test
  void storesFingerprintWithClassOriginAndApplicationSite()
      throws IOException, InterruptedException {
    request.setMethod("POST");
    request.setRequestURI("/api/notes/123");
    request.setQueryString("user=99");

    FailureReport report = createReport();

    assertThat(report.getFingerprint(), containsString("java.lang.RuntimeException"));
    assertThat(report.getFingerprint(), containsString("POST /api/notes/#"));
    assertThat(report.getFingerprint(), containsString("FailureReportFactoryTest.createReport"));
    assertThat(report.getFingerprint(), not(containsString("user=99")));
  }

  @Test
  void storesOccurrenceCountOfOne() throws IOException, InterruptedException {
    assertEquals(1, createReport().getOccurrenceCount());
  }

  @Test
  void storesFingerprintFromExceptionUsingSourceAsOrigin() {
    FailureReport report = createReportFromException();
    assertThat(
        report.getFingerprint(), containsString("source:QuestionGenerationBatchMaintenanceJob"));
  }

  @Test
  void coalescesConsecutiveSimilarFailuresIntoOneFailureReport()
      throws IOException, InterruptedException {
    createReportFromException("first");
    String firstDetail = failureReports().getFirst().getErrorDetail();

    createReportFromException("second");

    List<FailureReport> failureReports = failureReports();
    assertThat(failureReports, hasSize(1));
    assertEquals(2, failureReports.getFirst().getOccurrenceCount());
    assertEquals(firstDetail, failureReports.getFirst().getErrorDetail());
    verify(githubService, times(1)).createGithubIssue(any());
  }

  private FailureReport createReport() throws IOException, InterruptedException {
    CurrentUserFetcherFromRequest fetcher =
        new CurrentUserFetcherFromRequest(request, userRepository, userService, Optional.empty());

    new FailureReportFactory(
            request, new RuntimeException(), fetcher, githubService, failureReportRepository)
        .createUnlessAllowed();

    return failureReports().getFirst();
  }

  private FailureReport createReportFromException() {
    createReportFromException("for failure report");
    return failureReports().getFirst();
  }

  private void createReportFromException(String message) {
    FailureReportFactory.fromException(
            new RuntimeException(message),
            "QuestionGenerationBatchMaintenanceJob",
            githubService,
            failureReportRepository)
        .createUnlessAllowed();
  }

  private List<FailureReport> failureReports() {
    return StreamSupport.stream(failureReportRepository.findAll().spliterator(), false).toList();
  }
}
