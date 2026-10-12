package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RecallConversationContinuationControllerTest extends ConversationMessageControllerTestBase {
  @Autowired EntityManager entityManager;

  @Test
  void reopensTheSameAttemptWithItsStoredMessages() throws Exception {
    RecallPrompt prompt = ownedPrompt();
    Conversation first = controller.startConversationAboutRecallPrompt(prompt);
    controller.replyToConversation("My reasoning", first);
    entityManager.flush();
    entityManager.clear();

    Conversation reopened = controller.startConversationAboutRecallPrompt(prompt);

    assertThat(reopened.getId(), equalTo(first.getId()));
    assertThat(
        controller.getConversationMessages(reopened).stream()
            .map(message -> message.getMessage())
            .toList(),
        contains("My reasoning"));
  }

  @Test
  void differentAttemptsHaveTheirOwnDiscussions() throws Exception {
    RecallPrompt prompt = ownedPrompt();
    RecallPrompt other =
        makeMe
            .aRecallPrompt()
            .forMemoryTracker(prompt.getMemoryTracker())
            .withMcqForNote(prompt.getNote())
            .answerChoiceIndex(0)
            .please();

    Conversation first = controller.startConversationAboutRecallPrompt(prompt);
    Conversation second = controller.startConversationAboutRecallPrompt(other);

    assertThat(second.getId(), not(equalTo(first.getId())));
  }

  @Test
  void reusesTheOldestCompatiblePersonalDiscussion() throws Exception {
    RecallPrompt prompt = ownedPrompt();
    Conversation older =
        makeMe.aConversation().forARecallPrompt(prompt).from(currentUser.getUser()).please();
    makeMe.aConversation().forARecallPrompt(prompt).from(currentUser.getUser()).please();

    assertThat(
        controller.startConversationAboutRecallPrompt(prompt).getId(), equalTo(older.getId()));
  }

  @Test
  void keepsAnotherInitiatorsPersonalDiscussionAccessible() throws Exception {
    RecallPrompt prompt = ownedPrompt();
    User other = makeMe.aUser().please();
    Conversation shared = makeMe.aConversation().forARecallPrompt(prompt).from(other).please();

    Conversation privateHelp = controller.startConversationAboutRecallPrompt(prompt);
    assertThat(privateHelp.getId(), not(equalTo(shared.getId())));
    currentUser.setUser(other);
    assertThat(
        controller.replyToConversation("Other response", shared).getSender(), equalTo(other));
  }

  @Test
  void keepsLegacyCircleDiscussionSharedWhenStartingPrivateHelp() throws Exception {
    User member = makeMe.aUser().please();
    Note note =
        makeMe
            .aNote()
            .inCircle(makeMe.aCircle().hasMember(currentUser.getUser()).hasMember(member).please())
            .please();
    RecallPrompt prompt = promptFor(note);
    Conversation shared =
        makeMe.aConversation().forARecallPrompt(prompt).from(currentUser.getUser()).please();

    Conversation privateHelp = controller.startConversationAboutRecallPrompt(prompt);
    assertThat(privateHelp.getId(), not(equalTo(shared.getId())));
    currentUser.setUser(member);
    assertThat(
        controller.replyToConversation("Shared response", shared).getSender(), equalTo(member));
    assertThat(shared.getAccessOwnership(), equalTo(note.getNotebook().getOwnership()));
  }

  @Test
  void keepsLegacyAuthorDiscussionAccessibleWhenStartingPrivateHelp() throws Exception {
    User author = makeMe.aUser().please();
    Note note = makeMe.aNote().notebookOwnedBy(author).please();
    RecallPrompt prompt = promptFor(note);
    Conversation shared =
        makeMe.aConversation().forARecallPrompt(prompt).from(currentUser.getUser()).please();

    Conversation privateHelp = controller.startConversationAboutRecallPrompt(prompt);
    assertThat(privateHelp.getId(), not(equalTo(shared.getId())));
    currentUser.setUser(author);
    assertThat(
        controller.replyToConversation("Author response", shared).getSender(), equalTo(author));
    assertThat(shared.getAccessOwnership(), equalTo(author.getOwnership()));
  }

  private RecallPrompt ownedPrompt() {
    return promptFor(makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please());
  }

  private RecallPrompt promptFor(Note note) {
    return makeMe
        .aRecallPrompt()
        .forMemoryTracker(makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).please())
        .withMcqForNote(note)
        .answerChoiceIndex(1)
        .please();
  }
}
