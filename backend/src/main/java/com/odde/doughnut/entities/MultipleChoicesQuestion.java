package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MultipleChoicesQuestion {
  @JsonAlias({"f0__stem", "stem"})
  private String questionStem;

  @JsonAlias({"f1__choices", "choices"})
  private List<String> responseChoices;
}
