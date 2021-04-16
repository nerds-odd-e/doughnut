package com.odde.doughnut.configs;

import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class ControllerSetupTest {
    @Autowired
    MakeMe makeMe;
    @Autowired
    ModelFactoryService modelFactoryService;

    @BeforeEach
    void setup() {
        makeMe.modelFactoryService.failureReportRepository.deleteAll();
    }

    @Test
    void runtimeError () {
        ControllerSetup controllerSetup = new ControllerSetup(this.modelFactoryService);
        try {
            controllerSetup.handleSystemException(new RuntimeException());
            fail();
        } catch (Exception e) {
            Iterable<FailureReport> failureReportList = makeMe.modelFactoryService.failureReportRepository.findAll();

            for (FailureReport failureReport : failureReportList) {
                assertEquals(e.getClass().getName(), failureReport.getErrorName());
                assertEquals(Arrays.stream(e.getStackTrace()).findFirst().get().toString(), failureReport.getErrorDetail());
            }
        }
    }

}
