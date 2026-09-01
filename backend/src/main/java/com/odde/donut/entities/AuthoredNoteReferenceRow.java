package com.odde.donut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.donut.algorithms.AuthoredNoteReference;
import com.odde.donut.algorithms.PortablePath;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

/**
 * Persisted row for one {@link AuthoredNoteReference} authored in a source note's content (ADR 0001
 * Wiki link). Holds authored kind, the raw authored spelling, and kind-specific locator columns
 * needed for future candidate lookup — never a resolved destination-note relation for wiki targets.
 * See {@code authored_note_reference}'s {@code chk_authored_note_reference_kind_locator} for the
 * kind/locator invariant enforced at the database level.
 */
@Entity
@Table(name = "authored_note_reference")
public class AuthoredNoteReferenceRow extends EntityIdentifiedByIdOnly {

  public enum Kind {
    WIKI_PORTABLE_PATH,
    NOTE_ID_URL
  }

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "source_note_id", referencedColumnName = "id", nullable = false)
  @JsonIgnore
  @Getter
  @Setter
  private Note note;

  @Enumerated(EnumType.STRING)
  @Column(name = "kind", nullable = false, length = 32)
  @NotNull
  @Getter
  @Setter
  private Kind kind;

  @Column(name = "authored_link", nullable = false, length = 767)
  @NotNull
  @Size(max = 767)
  @Getter
  @Setter
  private String authoredLink;

  @Column(name = "display_text", nullable = false, length = 767)
  @NotNull
  @Size(max = 767)
  @Getter
  @Setter
  private String displayText;

  @Column(name = "document_order", nullable = false)
  @Getter
  @Setter
  private int documentOrder;

  @Column(name = "wiki_notebook_qualifier", length = 150)
  @Size(max = 150)
  @Getter
  @Setter
  private String wikiNotebookQualifier;

  @Column(name = "wiki_note_portion", length = 767)
  @Size(max = 767)
  @Getter
  @Setter
  private String wikiNotePortion;

  @Column(name = "wiki_encoded_property_key", length = 255)
  @Size(max = 255)
  @Getter
  @Setter
  private String wikiEncodedPropertyKey;

  @Column(name = "note_id_url_note_id")
  @Getter
  @Setter
  private Integer noteIdUrlNoteId;

  @Column(name = "note_id_url_href", length = 767)
  @Size(max = 767)
  @Getter
  @Setter
  private String noteIdUrlHref;

  /**
   * Builds the row for one authored reference, owned by {@code note}, at {@code documentOrder}.
   * Public so both {@link Note#replaceContent} (the aggregate content-save boundary) and the
   * one-time pre-existing-notes backfill ({@code AuthoredNoteReferenceBackfillTx}) share this
   * kind/locator mapping instead of duplicating it; only row construction is shared — the two
   * callers persist differently (aggregate collection vs. direct {@code EntityManager.persist}).
   */
  public static AuthoredNoteReferenceRow forSource(
      Note note, AuthoredNoteReference reference, int documentOrder) {
    AuthoredNoteReferenceRow row = new AuthoredNoteReferenceRow();
    row.setNote(note);
    row.setAuthoredLink(reference.authoredLink());
    row.setDisplayText(reference.displayText());
    row.setDocumentOrder(documentOrder);
    switch (reference) {
      case AuthoredNoteReference.WikiPortablePathTarget wiki -> {
        row.setKind(Kind.WIKI_PORTABLE_PATH);
        row.setWikiNotebookQualifier(wiki.portablePath().notebookQualifier().orElse(null));
        row.setWikiNotePortion(wiki.portablePath().notePortion());
        row.setWikiEncodedPropertyKey(wiki.portablePath().encodedPropertyKey().orElse(null));
      }
      case AuthoredNoteReference.NoteIdUrlTarget url -> {
        row.setKind(Kind.NOTE_ID_URL);
        row.setNoteIdUrlNoteId(url.noteId());
        row.setNoteIdUrlHref(url.href());
      }
    }
    return row;
  }

  /**
   * Reconstructs the domain {@link AuthoredNoteReference} from this row's stored kind and locator
   * columns, without re-parsing the source note's content.
   */
  public AuthoredNoteReference toDomainReference() {
    return switch (kind) {
      case WIKI_PORTABLE_PATH ->
          new AuthoredNoteReference.WikiPortablePathTarget(
              authoredLink,
              new PortablePath(
                  Optional.ofNullable(wikiNotebookQualifier),
                  wikiNotePortion,
                  Optional.ofNullable(wikiEncodedPropertyKey)),
              displayText);
      case NOTE_ID_URL ->
          new AuthoredNoteReference.NoteIdUrlTarget(
              authoredLink, noteIdUrlNoteId, noteIdUrlHref, displayText);
    };
  }
}
