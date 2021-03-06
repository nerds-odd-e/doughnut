package com.odde.doughnut.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.sql.Timestamp;

@Entity
@Table(name = "failure_report")
public class FailureReport {

    @Id @Getter @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;

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

    public GithubIssue getGithubIssue() {
        return new GithubIssue(getErrorName(), "Find the detail at: https://doughnut.odd-e.com/failure-report-list/show/" + id);
    }

    public static class GithubIssue {
        public String title;
        @Getter
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
