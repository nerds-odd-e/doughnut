package com.odde.doughnut.testability;

import com.odde.doughnut.entities.Certificate;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.models.UserModel;
import java.sql.Timestamp;

public class CertificateBuilder extends EntityBuilder<Certificate> {
  public CertificateBuilder(Notebook notebook, UserModel user, Timestamp startDate, MakeMe makeMe) {
    super(makeMe, new Certificate());
    entity.setNotebook(notebook);
    entity.setUser(user.getEntity());
    entity.setStartDate(startDate);
    entity.setExpiryDate(
        Timestamp.valueOf(
            startDate
                .toLocalDateTime()
                .plus(notebook.getNotebookSettings().getCertificateExpiry())));
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}
}
