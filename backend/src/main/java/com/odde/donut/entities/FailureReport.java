package com.odde.donut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "failure_report")
public class FailureReport {

  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @NotNull
  @Column(name = "error_name")
  @Getter
  @Setter
  private String errorName;

  @NotNull
  @Column(name = "error_detail")
  @Getter
  @Setter
  private String errorDetail;

  @Column(name = "issue_number")
  @Getter
  @Setter
  private Integer issueNumber;

  @NotNull
  @Column(name = "created_datetime")
  @Getter
  @Setter
  private Timestamp createDatetime = new Timestamp(System.currentTimeMillis());

  @JsonIgnore
  @Column(name = "fingerprint")
  @Getter
  @Setter
  private String fingerprint;

  @NotNull
  @Column(name = "occurrence_count")
  @Getter
  @Setter
  private Integer occurrenceCount = 1;

  @JsonIgnore
  @Column(name = "last_github_comment_datetime")
  @Getter
  @Setter
  private Timestamp lastGithubCommentDatetime;

  public GithubIssue getGithubIssue() {
    return new GithubIssue(
        getErrorName(),
        "Find the detail at: https://doughnut.odd-e.com/failure-report-list/show/" + id);
  }

  public static class GithubIssue {
    public String title;
    @Getter public String body;

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
