package com.odde.doughnut.entities;

import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Certificate {

  private User user;

  private Notebook notebook;

  private Timestamp startDate;

  private Timestamp expiryDate;
}
