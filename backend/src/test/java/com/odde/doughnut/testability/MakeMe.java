package com.odde.doughnut.testability;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.CircleModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.builders.*;
import java.sql.Timestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MakeMe extends MakeMeWithoutDB {
  @Autowired public ModelFactoryService modelFactoryService;

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
    return aNote().titleConstructor(title);
  }

  public NoteBuilder aHeadNote(String title) {
    return aNote().titleConstructor(title);
  }

  public NotebookBuilder aNotebook() {
    return new NotebookBuilder(null, this);
  }

  public NotebookBuilder theNotebook(Notebook notebook) {
    return new NotebookBuilder(notebook, this);
  }

  public NoteBuilder theNote(Note note) {
    return new NoteBuilder(note, this);
  }

  public BazaarNotebookBuilder aBazaarNotebook(Notebook notebook) {
    return new BazaarNotebookBuilder(this, notebook);
  }

  public CertificateBuilder aCertificate(Notebook notebook, UserModel user, Timestamp startDate) {

    return new CertificateBuilder(notebook, user, startDate, this);
  }

  public AssessmentAttemptBuilder anAssessmentAttempt(User currentUser) {
    AssessmentAttempt assessmentAttempt = new AssessmentAttempt();

    assessmentAttempt.setUser(currentUser);
    assessmentAttempt.setSubmittedAt(aTimestamp().please());
    assessmentAttempt.setTotalQuestionCount(2);
    assessmentAttempt.setAnswersCorrect(2);
    return new AssessmentAttemptBuilder(this, assessmentAttempt);
  }

  public <T> T refresh(T object) {
    modelFactoryService.entityManager.flush();
    modelFactoryService.entityManager.refresh(object);
    return object;
  }

  public MemoryTrackerBuilder aMemoryTrackerFor(Note note) {
    MemoryTracker memoryTracker = MemoryTracker.buildMemoryTrackerForNote(note);
    MemoryTrackerBuilder memoryTrackerBuilder = new MemoryTrackerBuilder(memoryTracker, this);
    memoryTrackerBuilder.entity.setNote(note);
    memoryTrackerBuilder.by(note.getCreator());
    return memoryTrackerBuilder;
  }

  public MemoryTrackerBuilder aMemoryTrackerBy(UserModel user) {
    Note note = aNote().please();
    return aMemoryTrackerFor(note).by(user);
  }

  public CircleBuilder aCircle() {
    return new CircleBuilder(null, this);
  }

  public CircleBuilder theCircle(CircleModel circleModel) {
    return new CircleBuilder(circleModel, this);
  }

  public ImageBuilder anImage() {
    return new ImageBuilder(new Image(), this);
  }

  public SubscriptionBuilder aSubscription() {
    return new SubscriptionBuilder(this, new Subscription());
  }

  public ReificationBuilder aReification() {
    return new ReificationBuilder(this);
  }

  public UserModel aNullUserModelPlease() {
    return modelFactoryService.toUserModel(null);
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

  public SuggestedQuestionForFineTuningBuilder aQuestionSuggestionForFineTunining() {
    return new SuggestedQuestionForFineTuningBuilder(this);
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
