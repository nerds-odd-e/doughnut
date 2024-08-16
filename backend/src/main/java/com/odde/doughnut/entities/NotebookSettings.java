package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class NotebookSettings {
  @Column(name = "skip_review_entirely")
  Boolean skipReviewEntirely = false;

  @Column(name = "number_of_questions_in_assessment")
  Integer numberOfQuestionsInAssessment;

  @Column(name = "until_cert_expire")
  Integer untilCertExpire = 1;

  @Column(name = "certified_by")
  String certifiedBy = "";

  @JsonIgnore
  public void update(NotebookSettings value) {
    setSkipReviewEntirely(value.getSkipReviewEntirely());
    setNumberOfQuestionsInAssessment(value.getNumberOfQuestionsInAssessment());
    setUntilCertExpire(value.getUntilCertExpire());
    setCertifiedBy(value.getCertifiedBy());
  }
}
