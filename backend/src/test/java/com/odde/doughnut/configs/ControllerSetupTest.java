package com.odde.doughnut.configs;

import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.services.GithubService;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

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

    @Test
    void catchException () throws IOException, InterruptedException {
        ControllerSetup controllerSetup = new ControllerSetup(githubService, this.modelFactoryService);
        when(githubService.createGithubIssue(any())).thenReturn(123);
        assertThrows(RuntimeException.class, ()-> controllerSetup.handleSystemException(new RuntimeException()));
        FailureReport failureReport = makeMe.modelFactoryService.failureReportRepository.findAll().iterator().next();
        assertEquals("java.lang.RuntimeException", failureReport.getErrorName());
        assertEquals(123, failureReport.getIssueNumber());
    }

}
