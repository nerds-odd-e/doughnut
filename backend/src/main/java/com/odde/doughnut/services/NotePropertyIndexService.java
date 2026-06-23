package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.NoteContentMarkdown;
import com.odde.doughnut.algorithms.PropertyKeyNaming;
import com.odde.doughnut.algorithms.WikiLinkMarkdown;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NotePropertyIndex;
import com.odde.doughnut.entities.repositories.NotePropertyIndexRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotePropertyIndexService {

  @PersistenceContext private EntityManager entityManager;

  private final NotePropertyIndexRepository notePropertyIndexRepository;
  private final WikiLinkResolver wikiLinkResolver;

  public NotePropertyIndexService(
      NotePropertyIndexRepository notePropertyIndexRepository, WikiLinkResolver wikiLinkResolver) {
    this.notePropertyIndexRepository = notePropertyIndexRepository;
    this.wikiLinkResolver = wikiLinkResolver;
  }

  @Transactional
  public void refreshForNote(Note note) {
    Integer noteId = note.getId();
    entityManager.find(Note.class, noteId, LockModeType.PESSIMISTIC_WRITE);
    notePropertyIndexRepository.deleteByNoteIdInBulk(noteId);
    entityManager.flush();
    Note indexOwner = entityManager.getReference(Note.class, noteId);
    NoteContentMarkdown.splitLeadingFrontmatter(note.getContent() == null ? "" : note.getContent())
        .ifPresent(
            lf ->
                lf.frontmatter()
                    .keys()
                    .forEach(
                        key -> {
                          if (!PropertyKeyNaming.isReservedStructuralKey(key)) {
                            String propertyValue = lf.frontmatter().getString(key).orElse(null);
                            NotePropertyIndex row = new NotePropertyIndex();
                            row.setNote(indexOwner);
                            row.setPropertyKey(key);
                            row.setItemIndex(0);
                            resolveTargetNoteFromPropertyValue(propertyValue, indexOwner)
                                .ifPresent(row::setTargetNote);
                            notePropertyIndexRepository.save(row);
                          }
                        }));
  }

  private Optional<Note> resolveTargetNoteFromPropertyValue(String value, Note focusNote) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    List<String> linkTokens = WikiLinkMarkdown.innerTitlesInOccurrenceOrder(value);
    if (linkTokens.isEmpty()) {
      return Optional.empty();
    }
    return wikiLinkResolver.resolveAnyTargetWikiLinkToken(linkTokens.getFirst(), focusNote);
  }
}
