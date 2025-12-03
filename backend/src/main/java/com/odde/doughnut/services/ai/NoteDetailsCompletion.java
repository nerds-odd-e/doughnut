package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@JsonClassDescription(
    """
    Generate a unified diff that updates ONLY the `details` field of the current

    focus note, refining, completing, or rewriting the note's details based on the
    user request. The details is in markdown format.

    Treat the existing details text exactly as given:
    - Every newline in the details corresponds to a line in the diff.
    - Preserve all existing line breaks and blank lines unless they are being
      explicitly removed.
    - Do NOT merge multiple lines into one or split one line into several unless
      that specific change is clearly required by the rewrite.

    Diff format requirements:
    - Use standard unified diff hunk syntax with a single hunk header line.
    - You MAY use a simple hunk header `@@` if you are not certain about exact
      line counts. If you include ranges, they must match the actual line counts.
    - Do NOT include file header lines (`---`, `+++`).
    - Each removed line must start with "-" immediately followed by the original
      line content.
    - Each added line must start with "+" immediately followed by the new line
      content.
    - Unchanged context lines (if any) must start with a single space " " and keep
      their content exactly.

    Editing rules:
    - Make only the minimal changes needed to apply the user's request.
    - Do not modify any fields other than the `details` text.
    - Do not reorder lines beyond what is strictly necessary.
    - Do not include any explanations, markdown fences, or extra text outside the
      diff itself.

    Example (multi-line edit):
    @@ -1,3 +1,2 @@
    -Old first paragraph line.
    -
    -Old second paragraph line.
    +New rewritten first paragraph.
    +New rewritten second paragraph.
    """)
@NoArgsConstructor
@AllArgsConstructor
public class NoteDetailsCompletion {

  @JsonPropertyDescription(
      "Unified diff text that transforms the current note’s details into the updated version.")
  @JsonProperty(required = true)
  public String patch;
}
