package com.odde.doughnut.controllers.json;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WikidataAssociationCreation {
  @NotNull public String wikidataId;
}
