package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.algorithms.Frontmatter;
import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.algorithms.NoteContentTitleHeading;
import com.odde.donut.algorithms.NoteLeadingFrontmatter;
import com.odde.donut.algorithms.RelationshipNoteComposition;
import com.odde.donut.algorithms.WikiLinkMarkdown;
import com.odde.donut.controllers.dto.NoteCreationDTO;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.ai.NoteExtractionResult;
import com.odde.donut.testability.TestabilitySettings;
import com.odde.donut.validators.AuthoredNoteContent;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NoteConstructionService {

  private final AuthorizationService authorizationService;
  private final TestabilitySettings testabilitySettings;
  private final FolderRepository folderRepository;
  private final EntityPersister entityPersister;
  private final NoteRealmService noteRealmService;
  private final NoteReferenceService noteReferenceService;
  private final NoteFactory noteFactory;
  private final CanonicalDonutOrigin canonicalDonutOrigin;
  private final AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence;
  private final NoteTitleNameRule noteTitleNameRule;
  private final FolderConstructionService folderConstructionService;
  private final WikiLinkResolver wikiLinkResolver;
  private final RelationshipMemoryTrackerRehoming memoryTrackerRehoming;

  @Autowired
  public NoteConstructionService(
      AuthorizationService authorizationService,
      TestabilitySettings testabilitySettings,
      FolderRepository folderRepository,
      EntityPersister entityPersister,
      NoteRealmService noteRealmService,
      NoteReferenceService noteReferenceService,
      NoteFactory noteFactory,
      CanonicalDonutOrigin canonicalDonutOrigin,
      AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence,
      NoteTitleNameRule noteTitleNameRule,
      FolderConstructionService folderConstructionService,
      WikiLinkResolver wikiLinkResolver,
      MemoryTrackerRepository memoryTrackerRepository) {
    this.authorizationService = authorizationService;
    this.testabilitySettings = testabilitySettings;
    this.folderRepository = folderRepository;
    this.entityPersister = entityPersister;
    this.noteRealmService = noteRealmService;
    this.noteReferenceService = noteReferenceService;
    this.noteFactory = noteFactory;
    this.canonicalDonutOrigin = canonicalDonutOrigin;
    this.authoredNoteDocumentPersistence = authoredNoteDocumentPersistence;
    this.noteTitleNameRule = noteTitleNameRule;
    this.folderConstructionService = folderConstructionService;
    this.wikiLinkResolver = wikiLinkResolver;
    this.memoryTrackerRehoming =
        new RelationshipMemoryTrackerRehoming(memoryTrackerRepository, entityPersister);
  }

  private Note persistNoteContent(Note note, String content) {
    applyContent(note, content);
    note.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
    return entityPersister.save(note);
  }

  /** Prepares {@code content} for save and replaces the note's Markdown and references from it. */
  private void applyContent(Note note, String content) {
    AuthoredNoteDocument document =
        AuthoredNoteContent.prepareDocumentForSave(content, canonicalDonutOrigin);
    note.replaceContent(document);
  }

  private void prependAndPersistWikidataDescription(Note note, String description) {
    applyContent(note, NoteLeadingFrontmatter.prependToBody(note.getContent(), description));
    entityPersister.save(note);
  }

  private Note buildNote(Notebook notebook, NoteCreationDTO noteCreation) {
    Folder folder = null;
    if (noteCreation.getFolderId() != null) {
      folder =
          folderRepository
              .findById(noteCreation.getFolderId())
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found."));
      folder.requireInNotebook(notebook);
    }
    if (noteCreation.getChildFolderName() != null) {
      folder =
          folderConstructionService.folderToEnterOrCreate(
              notebook, folder, new DisplayName(noteCreation.getChildFolderName()));
    }
    noteTitleNameRule.requireTitleFree(notebook, folder, noteCreation.getNewTitle());
    Note note = noteFactory.create(notebook, folder, noteCreation.getNewTitle());
    if (noteCreation.getContent() != null) {
      persistNoteContent(note, noteCreation.getContent());
    }
    return note;
  }

  /** Final refresh, reference indexing and response construction. */
  private NoteRealm finalizeAndRespond(Note note, User user) {
    entityPersister.flush();
    entityPersister.refresh(note);
    noteReferenceService.refreshDerivedIndexesForNote(note);
    return noteRealmService.build(note, user);
  }

  public NoteRealm createRootNote(
      Notebook notebook,
      NoteCreationDTO noteCreation,
      User user,
      Optional<String> wikidataDescription) {
    Note note = buildNote(notebook, noteCreation);
    wikidataDescription.ifPresent(
        description -> prependAndPersistWikidataDescription(note, description));
    return finalizeAndRespond(note, user);
  }

  public NoteRealm createNoteFromExtractedSuggestion(
      Note originalNote, NoteExtractionResult aiResult) {
    User user = authorizationService.getCurrentUser();

    String newNoteContent =
        NoteContentTitleHeading.withoutRepeatedTitleHeading(
            aiResult.newNoteTitle, aiResult.newNoteContent);

    noteTitleNameRule.requireTitleFree(
        originalNote.getNotebook(), originalNote.getFolder(), aiResult.newNoteTitle);
    Note newNote =
        noteFactory.create(
            originalNote.getNotebook(), originalNote.getFolder(), aiResult.newNoteTitle);
    persistAuthoredContent(newNote, newNoteContent);
    persistAuthoredContent(originalNote, aiResult.updatedOriginalNoteContent);

    return noteRealmService.build(newNote, user);
  }

  /**
   * Turns the {@code propertyKey} property of {@code source}, whose value is a wiki link to a note,
   * into a relationship note beside {@code source}, moves every learner's tracker of that property
   * onto the new note as a note-level tracker, and removes the property from {@code source}.
   * Returns the new relationship note.
   */
  public Note reifyPropertyIntoRelationshipNote(Note source, String propertyKey, User viewer) {
    Frontmatter frontmatter =
        NoteContentMarkdown.splitLeadingFrontmatter(source.getContent())
            .map(NoteContentMarkdown.LeadingFrontmatter::frontmatter)
            .orElseGet(Frontmatter::empty);
    if (!frontmatter.containsKeyIgnoreCase(propertyKey)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "The note has no property " + propertyKey + ".");
    }
    String targetLink =
        frontmatter
            .getString(propertyKey)
            .map(String::trim)
            .filter(WikiLinkMarkdown::isWellFormedWholeLinkToken)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Only a property whose value is a link to a note can be reified."));
    Note target =
        wikiLinkResolver
            .resolveFirstWikiLink(targetLink, source, viewer)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "The link " + targetLink + " does not name an existing note."));
    String title =
        RelationshipNoteComposition.title(source.getTitle(), propertyKey, target.getTitle());
    noteTitleNameRule.requireTitleFree(source.getNotebook(), source.getFolder(), title);
    Note relationshipNote = noteFactory.create(source.getNotebook(), source.getFolder(), title);
    persistAuthoredContent(
        relationshipNote,
        RelationshipNoteComposition.markdown(
            propertyKey, "[[" + source.getTitle() + "]]", targetLink));
    memoryTrackerRehoming.movePropertyTrackersOntoRelationshipNote(
        source, propertyKey, relationshipNote);
    persistAuthoredContent(
        source, NoteContentMarkdown.removeFrontmatterProperty(source.getContent(), propertyKey));
    return relationshipNote;
  }

  private void persistAuthoredContent(Note note, String content) {
    AuthoredNoteDocument document =
        AuthoredNoteContent.prepareDocumentForSave(content, canonicalDonutOrigin);
    authoredNoteDocumentPersistence.persist(
        note, document, testabilitySettings.getCurrentUTCTimestamp());
  }
}
