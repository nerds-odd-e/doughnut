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
    @Size(min = 1, max = 100)
    @Getter
    @Setter
    private String errorName;

    @NotNull
    @Column(name = "error_detail")
    @Size(min = 1, max = 1000)
    @Getter
    @Setter
    private String errorDetail;

    @NotNull
    @Column(name = "issue_number")
    @Getter
    @Setter
    private Integer issueNumber;

    @NotNull
    @Column(name = "created_datetime")
    @Getter
    @Setter
    private Timestamp createDatetime = new Timestamp(System.currentTimeMillis());

    public FailureReport() {
    }
}
