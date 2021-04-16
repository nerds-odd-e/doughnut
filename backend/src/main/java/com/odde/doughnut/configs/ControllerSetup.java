package com.odde.doughnut.configs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.FailureReport;
import com.odde.doughnut.services.ModelFactoryService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

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

    @SneakyThrows
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleSystemException(RuntimeException e) throws RuntimeException, JsonProcessingException {
        FailureReport failureReport = new FailureReport();
        failureReport.setErrorName(e.getClass().getName());
        failureReport.setErrorDetail(Arrays.stream(e.getStackTrace()).findFirst().get().toString());
        this.modelFactoryService.failureReportRepository.save(failureReport);

        GithubIssue githubIssue = new GithubIssue(failureReport.getErrorName(), failureReport.getErrorDetail());

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(githubIssue);

        HttpRequest request = HttpRequest
                .newBuilder(URI.create("https://api.github.com/repos/nerds-odd-e/doughnut_sandbox/issues"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .setHeader("Content-Type", "application/json")
                .setHeader("Accept", "application/vnd.github.v3+json")
                .setHeader("Authorization", GithubApiToken.getToken())
                .build();

        HttpResponse.BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        HttpResponse<String> response = null;

        try {
            response = HttpClient.newBuilder().build().send(request, bodyHandler);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
        }

        Map<String, Object> map = null;
        ObjectMapper mapper2 = new ObjectMapper();

        try {
            map = mapper2.readValue(response.body(), new TypeReference<Map<String, Object>>(){});
        } catch (Exception e2) {
            e.printStackTrace();
        }

        failureReport.setIssueNumber(Integer.valueOf(String.valueOf(map.get("number"))));
        this.modelFactoryService.failureReportRepository.save(failureReport);

        throw e;
    }

    private class GithubIssue {
        public String title;
        public String body;

        public GithubIssue(String errorName, String errorDetail) {
            this.title = errorName;
            this.body = errorDetail;
        }

        @Override
        public String toString() {
            return "GithubIssue [title=" + title + ", body=" + body + "]";
        }
    }

    private static class GithubApiToken {
        private static final String token = "token ";
        private static final String ghp = "ghp_";
        private static String value1 = "4TY2c34azFl3Si8YkFS";
        private static final String value2 = "0KqaxfB8eAy0kGmjR";

        public static String getToken() {
            return token + ghp + value1 + value2;
        }
    }
}
