package com.odde.donut.testability;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.algorithms.AuthoredNoteReferences;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.entities.*;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.NoteAliasIndexService;
import com.odde.donut.services.NoteEmbeddingService;
import com.odde.donut.services.NoteLevelIndexService;
import com.odde.donut.services.book.BookStorage;
import com.odde.donut.testability.builders.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MakeMe extends MakeMeWithoutDB {
  @Autowired public EntityPersister entityPersister;
  @Autowired public NoteEmbeddingService noteEmbeddingService;
  @Autowired public NoteAliasIndexService noteAliasIndexService;
  @Autowired public NoteLevelIndexService noteLevelIndexService;
  @Autowired public BookStorage bookStorage;
  @Autowired public TestabilitySettings testabilitySettings;

  private MakeMe() {}

  public static MakeMe makeMeWithoutFactoryService() {
    return new MakeMe();
  }

  public UserBuilder aUser() {
    return new UserBuilder(this);
  }

  public UserBuilder aUser(String userName) {
    return new UserBuilder(this, userName);
  }

  public UserBuilder theUser(User user) {
    return new UserBuilder(this, user);
  }

  public UserBuilder anAdmin() {
    return new UserBuilder(this, "admin");
  }

  public UserTokenBuilder aUserToken() {
    return new UserTokenBuilder(this);
  }

  public NoteBuilder aNote() {
    return new NoteBuilder(this);
  }

  public NoteBuilder aNote(String title) {
    return aNote().title(title);
  }

  public NoteBuilder aRootNote(String title) {
    return aNote(title);
  }

  public NotebookBuilder aNotebook() {
    return new NotebookBuilder(null, this);
  }

  public NotebookBuilder theNotebook(Notebook notebook) {
    return new NotebookBuilder(notebook, this);
  }

  public NotebookGroupBuilder aNotebookGroup() {
    return new NotebookGroupBuilder(this, null);
  }

  public BookBuilder aBook() {
    return new BookBuilder(this);
  }

  public NoteBuilder theNote(Note note) {
    return new NoteBuilder(note, this);
  }

  public BazaarNotebookBuilder aBazaarNotebook(Notebook notebook) {
    return new BazaarNotebookBuilder(this, notebook);
  }

  public FolderBuilder aFolder() {
    return new FolderBuilder(this, null);
  }

  public FolderBuilder theFolder(Folder folder) {
    return new FolderBuilder(this, folder);
  }

  public <T> T refresh(T object) {
    entityPersister.flush();
    entityPersister.refresh(object);
    return object;
  }

  /**
   * Authors {@code content} on {@code note} through {@link Note#replaceContent}, the aggregate
   * method that also populates {@code authored_note_reference} rows consumed by {@link
   * com.odde.donut.entities.repositories.AuthoredNoteReferenceInboundFacade} — the same parse
   * production content saves use, minus validation. Test builders' {@code .content(...)} sets raw
   * content only, on purpose bypassing reference parsing — use this instead whenever a test needs
   * the note to be discoverable as an inbound referrer.
   */
  public void authorReferencingContent(Note note, String content) {
    note.replaceContent(
        new AuthoredNoteDocument(
            content,
            AuthoredNoteReferences.uniquePreserveOrder(
                AuthoredNoteReferences.inOccurrenceOrder(
                    content, CanonicalDonutOrigin.production()))));
    entityPersister.flush();
  }

  public MemoryTrackerBuilder aMemoryTrackerFor(Note note) {
    MemoryTracker memoryTracker = MemoryTracker.buildMemoryTrackerForNote(note);
    MemoryTrackerBuilder memoryTrackerBuilder = new MemoryTrackerBuilder(memoryTracker, this);
    memoryTrackerBuilder.entity.setNote(note);
    memoryTrackerBuilder.by(note.getNotebook().getOwnership().getUser());
    return memoryTrackerBuilder;
  }

  public AssimilationSequenceSkipBuilder anAssimilationSequenceSkipFor(Note note) {
    AssimilationSequenceSkip skip = new AssimilationSequenceSkip();
    skip.setNote(note);
    AssimilationSequenceSkipBuilder builder = new AssimilationSequenceSkipBuilder(skip, this);
    builder.by(note.getNotebook().getOwnership().getUser());
    return builder;
  }

  public MemoryTrackerBuilder aMemoryTrackerBy(User user) {
    Note note = aNote().please();
    return aMemoryTrackerFor(note).by(user);
  }

  public CircleBuilder aCircle() {
    return new CircleBuilder(null, this);
  }

  public CircleBuilder theCircle(Circle circle) {
    return new CircleBuilder(circle, this);
  }

  public ImageBuilder anImage() {
    return new ImageBuilder(new Image(), this);
  }

  public SubscriptionBuilder aSubscription() {
    return new SubscriptionBuilder(this, new Subscription());
  }

  public McqBuilder anMcq() {
    return new McqBuilder(this);
  }

  public RecallPromptBuilder aRecallPrompt() {
    return new RecallPromptBuilder(this, null);
  }

  public FailureReportBuilder aFailureReport() {
    return new FailureReportBuilder(this);
  }

  public DailyProbeBuilder aDailyProbe() {
    return new DailyProbeBuilder(this);
  }

  public ConversationBuilder aConversation() {
    return new ConversationBuilder(this);
  }

  public ConversationMessageBuilder aConversationMessage(Conversation conversation) {
    return new ConversationMessageBuilder(conversation, this);
  }

  public NoteEmbeddingBuilder aNoteEmbedding(Note note) {
    return new NoteEmbeddingBuilder(note, this);
  }

  public QuestionGenerationBatchBuilder aQuestionGenerationBatch() {
    return new QuestionGenerationBatchBuilder(this);
  }

  public QuestionGenerationBatchRequestBuilder aQuestionGenerationBatchRequest() {
    return new QuestionGenerationBatchRequestBuilder(this);
  }

  public RecallLogBuilder aRecallLogFor(MemoryTracker memoryTracker) {
    return new RecallLogBuilder(this).memoryTracker(memoryTracker);
  }
}
