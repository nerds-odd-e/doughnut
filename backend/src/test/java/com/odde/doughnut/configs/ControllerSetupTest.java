package com.odde.doughnut.configs;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.GithubService;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class ControllerSetupTest {
    @Autowired
    MakeMe makeMe;
    @Autowired
    ModelFactoryService modelFactoryService;
    @Mock
    GithubService githubService;
    @Mock
    CurrentUserFetcher currentUserFetcher;
    MockHttpServletRequest request = new MockHttpServletRequest();

    ControllerSetup controllerSetup;

    @BeforeEach
    void setup() {
        controllerSetup = new ControllerSetup(githubService, this.modelFactoryService, currentUserFetcher);
    }

    @Test
    void shouldRecordExceptionDetails() {
        FailureReport failureReport = catchExceptionAndGetFailureReport();
        assertEquals("java.lang.RuntimeException", failureReport.getErrorName());
        assertThat(failureReport.getErrorDetail(), containsString("ControllerSetupTest.java"));
    }

    @Test
    void shouldCreateGithubIssue() throws IOException, InterruptedException {
        when(githubService.createGithubIssue(any())).thenReturn(123);
        FailureReport failureReport = catchExceptionAndGetFailureReport();
        assertEquals(123, failureReport.getIssueNumber());
    }

    @Test
    void shouldRecordUserInfo() {
        UserModel userModel = makeMe.aUser().toModelPlease();
        String externalId = userModel.getEntity().getExternalIdentifier();
        when(currentUserFetcher.getExternalIdentifier()).thenReturn(externalId);
        when(currentUserFetcher.getUser()).thenReturn(userModel);
        FailureReport failureReport = catchExceptionAndGetFailureReport();
        assertThat(failureReport.getErrorDetail(), containsString(externalId));
        assertThat(failureReport.getErrorDetail(), containsString(userModel.getName()));
    }

    @Test
    void shouldRecordRequestInfo() {
        request.setRequestURI("/path");
        FailureReport failureReport = catchExceptionAndGetFailureReport();
        assertThat(failureReport.getErrorDetail(), containsString("/path"));
    }

    private FailureReport catchExceptionAndGetFailureReport() {
        assertThrows(RuntimeException.class, ()-> controllerSetup.handleSystemException(request, new RuntimeException()));
        return makeMe.modelFactoryService.failureReportRepository.findAll().iterator().next();
    }
}
