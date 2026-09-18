package com.odde.donut.controllers.dto;

import java.util.List;
import lombok.Value;

@Value
public class FailureReportDeletionResultDTO {
  List<String> unresolvedGithubIssueUrls;
}
