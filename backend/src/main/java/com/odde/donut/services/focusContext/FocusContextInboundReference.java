package com.odde.donut.services.focusContext;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * One entry of the focus note's {@code inboundReferences}: the wiki link that selects the referring
 * note, plus the property types of the referring note's properties that hold its link to the focus
 * note. {@code referrerPropertyTypes} is null (omitted) when no indexed property holds the link.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FocusContextInboundReference(
    @Schema(description = "Wiki-style link string that selects the referring note.") String link,
    @Schema(
            description =
                "Distinct property types (key without numeric suffix), in authored order, of the"
                    + " referring note's own properties that link to the focus note. Omitted when"
                    + " none.")
        List<String> referrerPropertyTypes) {

  public static FocusContextInboundReference of(String link, List<String> referrerPropertyTypes) {
    return new FocusContextInboundReference(
        link, referrerPropertyTypes.isEmpty() ? null : referrerPropertyTypes);
  }
}
