package com.odde.donut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * One References entry: the referring note's topology plus the property types the referring note
 * holds its reference(s) to the target in. {@code propertyTypes} is null (omitted) when the
 * reference is held by no indexed property.
 */
public record NoteReferenceEntry(
    @JsonUnwrapped NoteTopology noteTopology,
    @Schema(
            description =
                "Distinct property types (key without numeric suffix), in the referring note's"
                    + " authored order, of the referring note's properties that link to the"
                    + " target. Omitted when none.")
        List<String> propertyTypes) {

  public static NoteReferenceEntry of(NoteTopology noteTopology, List<String> propertyTypes) {
    return new NoteReferenceEntry(noteTopology, propertyTypes.isEmpty() ? null : propertyTypes);
  }
}
