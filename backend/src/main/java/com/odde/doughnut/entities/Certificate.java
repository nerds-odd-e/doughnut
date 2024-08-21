package com.odde.doughnut.entities;

import java.sql.Date;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Certificate {

  private User user;

  private Notebook notebook;

  private Date startDate;

  private Date expiryDate;
}
