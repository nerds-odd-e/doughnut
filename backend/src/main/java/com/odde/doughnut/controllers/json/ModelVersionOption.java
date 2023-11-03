package com.odde.doughnut.controllers.json;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder({"key", "value", "label"})
public class ModelVersionOption {
  String Key;
  String Value;
  String Label;
}
