package com.odde.doughnut.entities;

public enum ApprovalStatus {
  NOT_APPROVED("Not approved"),
  PENDING("Pending"),
  APPROVED("Approved");

  public final String label;

  ApprovalStatus(String label) {
    this.label = label;
  }
}
