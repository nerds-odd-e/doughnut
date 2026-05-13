package com.odde.doughnut.testability;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.NoteEmbeddingService;
import com.odde.doughnut.services.WikiTitleCacheService;
import com.odde.doughnut.services.book.BookStorage;
import com.odde.doughnut.testability.builders.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MakeMe extends MakeMeWithoutDB {
  @Autowired public EntityPersister entityPersister;
  @Autowired public WikiTitleCacheService wikiTitleCacheService;
  @Autowired public NoteEmbeddingService noteEmbeddingService;
  @Autowired public BookStorage bookStorage;
  @Autowired public TestabilitySettings testabilitySettings;

  private MakeMe() {}

  public static MakeMe makeMeWithoutFactoryService() {
    return new MakeMe();
  }

  public UserBuilder aUser() {
    return new UserBuilder(this);
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

  public MemoryTrackerBuilder aMemoryTrackerFor(Note note) {
    MemoryTracker memoryTracker = MemoryTracker.buildMemoryTrackerForNote(note);
    MemoryTrackerBuilder memoryTrackerBuilder = new MemoryTrackerBuilder(memoryTracker, this);
    memoryTrackerBuilder.entity.setNote(note);
    memoryTrackerBuilder.by(note.getNotebook().getOwnership().getUser());
    return memoryTrackerBuilder;
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

  public PredefinedQuestionBuilder aPredefinedQuestion() {
    return new PredefinedQuestionBuilder(this);
  }

  public RecallPromptBuilder aRecallPrompt() {
    return new RecallPromptBuilder(this, null);
  }

  public RecallPromptBuilder theRecallPrompt(RecallPrompt recallPrompt) {
    return new RecallPromptBuilder(this, recallPrompt);
  }

  public FailureReportBuilder aFailureReport() {
    return new FailureReportBuilder(this);
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
}
