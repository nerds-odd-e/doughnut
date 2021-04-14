package com.odde.doughnut.configs;

import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.services.ModelFactoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Arrays;

@ControllerAdvice
public class ControllerSetup
{
    @Autowired
    public ModelFactoryService modelFactoryService;

    public ControllerSetup(ModelFactoryService modelFactoryService) {
        this.modelFactoryService = modelFactoryService;
    }

    @InitBinder
    public void initBinder ( WebDataBinder binder )
    {
        StringTrimmerEditor stringTrimmerEditor = new StringTrimmerEditor(false);
        binder.registerCustomEditor(String.class, stringTrimmerEditor);
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleSystemException(RuntimeException e) throws RuntimeException {

        // ExceptionHandling
        FailureReport failureReport = new FailureReport();
        failureReport.setErrorName(e.getClass().getName());
        failureReport.setErrorDetail(Arrays.stream(e.getStackTrace()).findFirst().get().toString());
        this.modelFactoryService.failureReportRepository.save(failureReport);

        throw e;
    }
}
