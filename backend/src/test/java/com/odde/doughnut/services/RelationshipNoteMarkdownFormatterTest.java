package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.RelationType;
import org.junit.jupiter.api.Test;

class RelationshipNoteMarkdownFormatterTest {

  @Test
  void formats_frontmatter_and_body_for_confused_with() {
    String markdown =
        RelationshipNoteMarkdownFormatter.format(RelationType.CONFUSE_WITH, "A", "B", null);
    assertThat(markdown, containsString("type: relationship"));
    assertThat(markdown, containsString("relation: confused-with"));
    assertThat(markdown, containsString("source: \"[[A]]\""));
    assertThat(markdown, containsString("target: \"[[B]]\""));
    assertThat(markdown, containsString("[[A]] confused with [[B]]."));
  }

  @Test
  void blank_source_and_target_use_untitled_in_frontmatter_and_body() {
    String markdown = RelationshipNoteMarkdownFormatter.format(RelationType.PART, "", "  ", null);
    assertThat(markdown, containsString("source: \"[[Untitled]]\""));
    assertThat(markdown, containsString("target: \"[[Untitled]]\""));
    assertThat(markdown, containsString("[[Untitled]] a part of [[Untitled]]."));
  }

  @Test
  void null_relation_type_defaults_to_related_to() {
    String markdown = RelationshipNoteMarkdownFormatter.format(null, "X", "Y", null);
    assertThat(markdown, containsString("relation: related-to"));
    assertThat(markdown, containsString("[[X]] related to [[Y]]."));
  }

  @Test
  void relationKebabFromLabel_matches_format_output() {
    assertThat(
        RelationshipNoteMarkdownFormatter.relationKebabFromLabel(RelationType.CONFUSE_WITH.label),
        equalTo("confused-with"));
  }

  @Test
  void appends_preserved_details_below_body() {
    String markdown =
        RelationshipNoteMarkdownFormatter.format(
            RelationType.RELATED_TO, "S", "T", "  Legacy line one.\n\nLine two.  ");
    assertThat(
        markdown,
        equalTo(
            "---\n"
                + "type: relationship\n"
                + "relation: related-to\n"
                + "source: \"[[S]]\"\n"
                + "target: \"[[T]]\"\n"
                + "---\n\n"
                + "[[S]] related to [[T]].\n\n"
                + "Legacy line one.\n\nLine two."));
  }

  @Test
  void extractUserSuffix_returns_null_when_no_trailing_user_content() {
    String markdown =
        RelationshipNoteMarkdownFormatter.format(RelationType.RELATED_TO, "S", "T", null);
    assertThat(
        RelationshipNoteMarkdownFormatter.extractUserSuffixFromRelationshipDetails(markdown),
        equalTo(null));
  }

  @Test
  void extractUserSuffix_returns_content_after_generated_block() {
    String markdown =
        RelationshipNoteMarkdownFormatter.format(
            RelationType.PART, "Moon", "Earth", "My crater note.\n\nSecond paragraph.");
    assertThat(
        RelationshipNoteMarkdownFormatter.extractUserSuffixFromRelationshipDetails(markdown),
        equalTo("My crater note.\n\nSecond paragraph."));
  }

  @Test
  void extractUserSuffix_handles_crlf_in_input() {
    String lf = RelationshipNoteMarkdownFormatter.format(RelationType.PART, "A", "B", "user line");
    String crlf = lf.replace("\n", "\r\n");
    assertThat(
        RelationshipNoteMarkdownFormatter.extractUserSuffixFromRelationshipDetails(crlf),
        equalTo("user line"));
  }

  @Test
  void extractUserSuffix_returns_null_when_not_relationship_frontmatter() {
    assertThat(
        RelationshipNoteMarkdownFormatter.extractUserSuffixFromRelationshipDetails(
            "---\ntype: article\n---\n\nBody.\n\nExtra"),
        equalTo(null));
  }

  @Test
  void escapes_quotes_and_backslashes_in_yaml_quoted_titles() {
    String markdown =
        RelationshipNoteMarkdownFormatter.format(
            RelationType.RELATED_TO, "Say \"hi\"", "C:\\path", null);
    assertThat(markdown, containsString("source: \"[[Say \\\"hi\\\"]]\""));
    assertThat(markdown, containsString("target: \"[[C:\\\\path]]\""));
    assertThat(markdown, startsWith("---\n"));
  }

  @Test
  void formatForRelationshipNote_uses_unqualified_links_when_notebooks_match() {
    Notebook nb = newNotebook("OnlyNb");
    Note source = noteIn(nb, "Alpha");
    Note target = noteIn(nb, "Beta");
    Note relation = noteIn(nb, "");
    String markdown =
        RelationshipNoteMarkdownFormatter.formatForRelationshipNote(
            relation, RelationType.RELATED_TO, source, target, null);
    assertThat(markdown, containsString("source: \"[[Alpha]]\""));
    assertThat(markdown, containsString("target: \"[[Beta]]\""));
    assertThat(markdown, containsString("[[Alpha]] related to [[Beta]]."));
  }

  @Test
  void formatForRelationshipNote_qualifies_endpoint_in_other_notebook() {
    Notebook nbRel = newNotebook("HomeNb");
    Notebook nbOther = newNotebook("OtherNb");
    Note source = noteIn(nbRel, "Source");
    Note target = noteIn(nbOther, "Target");
    Note relation = noteIn(nbRel, "");
    String markdown =
        RelationshipNoteMarkdownFormatter.formatForRelationshipNote(
            relation, RelationType.PART, source, target, null);
    assertThat(markdown, containsString("source: \"[[Source]]\""));
    assertThat(markdown, containsString("target: \"[[OtherNb: Target]]\""));
    assertThat(markdown, containsString("[[Source]] a part of [[OtherNb: Target]]."));
  }

  @Test
  void formatForRelationshipNote_appends_preserved_suffix() {
    Notebook nb = newNotebook("N");
    Note source = noteIn(nb, "A");
    Note target = noteIn(nb, "B");
    Note relation = noteIn(nb, "");
    String markdown =
        RelationshipNoteMarkdownFormatter.formatForRelationshipNote(
            relation, RelationType.RELATED_TO, source, target, "User bit.\n");
    assertThat(markdown, containsString("[[A]] related to [[B]].\n\nUser bit."));
  }

  private static Notebook newNotebook(String name) {
    Notebook nb = new Notebook();
    nb.setName(name);
    return nb;
  }

  private static Note noteIn(Notebook nb, String title) {
    Note n = new Note();
    n.assignNotebook(nb);
    n.setTitle(title);
    return n;
  }
}
