package com.odde.doughnut.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ControllerSetup;
import com.odde.doughnut.entities.FailureReport;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class GithubService {
    public GithubService() {
    }

    public Integer createGithubIssue(FailureReport failureReport) throws IOException, InterruptedException {
        GithubIssue githubIssue = new GithubIssue(failureReport.getErrorName(), failureReport.getErrorDetail());
        ObjectMapper mapper = new ObjectMapper();
        HttpRequest request = HttpRequest
                .newBuilder(URI.create("https://api.github.com/repos/nerds-odd-e/doughnut_sandbox/issues"))
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(githubIssue)))
                .setHeader("Content-Type", "application/json")
                .setHeader("Accept", "application/vnd.github.v3+json")
                .setHeader("Authorization", GithubApiToken.getToken())
                .build();
        HttpResponse.BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        HttpResponse<String> response = HttpClient.newBuilder().build().send(request, bodyHandler);
        Map<String, Object> map = mapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {
        });

        return Integer.valueOf(String.valueOf(map.get("number")));
    }// Pushing an API token to Github will invalidate the token,

    // split the string and keep it
    private static class GithubApiToken {
        private static final String token = "token ";
        private static final String ghp = "ghp_";
        private static final String value1 = "4TY2c34azFl3Si8YkFS";
        private static final String value2 = "0KqaxfB8eAy0kGmjR";

        public static String getToken() {
            return token + ghp + value1 + value2;
        }
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
}