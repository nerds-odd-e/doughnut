package com.odde.doughnut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WikiSlugMigrationStatus {
  private long foldersMissingSlug;
  private long notesMissingSlug;
}
