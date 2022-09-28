package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import com.odde.doughnut.entities.Link.LinkType;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteAccessories;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.*;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.TimestampOperations;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.HttpClientAdapter;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class RestNoteControllerTests {
  @Autowired ModelFactoryService modelFactoryService;

  @Autowired MakeMe makeMe;
  @Mock HttpClientAdapter httpClientAdapter;
  private UserModel userModel;
  RestNoteController controller;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    controller =
        new RestNoteController(
            modelFactoryService, userModel, httpClientAdapter, testabilitySettings);
  }

  @Nested
  class showNoteTest {
    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      User otherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(otherUser).please();
      assertThrows(NoAccessRightException.class, () -> controller.show(note));
    }

    @Test
    void shouldReturnTheNoteInfoIfHavingReadingAuth() throws NoAccessRightException {
      User otherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(otherUser).please();
      makeMe.aBazaarNodebook(note.getNotebook()).please();
      makeMe.refresh(userModel.getEntity());
      final NoteRealmWithPosition show = controller.show(note);
      assertThat(show.noteRealm.getNote().getTitle(), equalTo(note.getTitle()));
      assertThat(show.notePosition.getNotebook().getFromBazaar(), is(true));
    }

    @Test
    void shouldBeAbleToSeeOwnNote() throws NoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(userModel).please();
      makeMe.refresh(userModel.getEntity());
      final NoteRealmWithPosition show = controller.show(note);
      assertThat(show.noteRealm.getId(), equalTo(note.getId()));
      assertThat(show.notePosition.getNotebook().getFromBazaar(), is(false));
    }

    @Test
    void shouldBeAbleToSeeOwnNoteOverview() throws NoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(userModel).please();
      Note childNote = makeMe.aNote().creatorAndOwner(userModel).under(note).please();
      makeMe.theNote(childNote).with10Children().please();
      makeMe.refresh(note);
      makeMe.refresh(childNote);
      final NoteRealmWithAllDescendants showOverview = controller.showOverview(note);
      assertThat(showOverview.notes, hasSize(12));
      assertThat(showOverview.notePosition.getNotebook().getFromBazaar(), equalTo(false));
      assertThat(showOverview.notes.get(0).getChildren(), hasSize(1));
    }
  }

  @Nested
  class showStatistics {
    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      User otherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(otherUser).please();
      assertThrows(NoAccessRightException.class, () -> controller.getNoteInfo(note));
    }

    @Test
    void shouldReturnTheNoteInfoIfHavingReadingAuth() throws NoAccessRightException {
      User otherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(otherUser).please();
      makeMe
          .aSubscription()
          .forUser(userModel.getEntity())
          .forNotebook(note.getNotebook())
          .please();
      makeMe.refresh(userModel.getEntity());
      assertThat(controller.getNoteInfo(note).getNote().getId(), equalTo(note.getId()));
    }
  }

  @Nested
  class createNoteTest {
    Note parent;
    NoteCreation noteCreation = new NoteCreation();

    @BeforeEach
    void setup() {
      parent = makeMe.aNote().creatorAndOwner(userModel).please();
      Note newNote = makeMe.aNote().inMemoryPlease();
      noteCreation.setTextContent(newNote.getTextContent());
      noteCreation.setLinkTypeToParent(LinkType.NO_LINK);
    }

    @Test
    void shouldBeAbleToSaveNoteWhenValid()
        throws NoAccessRightException, BindException, InterruptedException {
      NoteRealmWithPosition response = controller.createNote(parent, noteCreation);
      assertThat(response.noteRealm.getId(), not(nullValue()));
    }

    @Test
    void shouldBeAbleToCreateAThing()
        throws NoAccessRightException, BindException, InterruptedException {
      long beforeThingCount = makeMe.modelFactoryService.thingRepository.count();
      controller.createNote(parent, noteCreation);
      long afterThingCount = makeMe.modelFactoryService.thingRepository.count();
      assertThat(afterThingCount, equalTo(beforeThingCount + 1));
    }

    @Test
    void shouldBeAbleToSaveNoteWithWikidataIdWhenValid()
        throws NoAccessRightException, BindException, InterruptedException, IOException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(new MakeMe().wikidataEntityJson().entityId("Q12345").please());
      noteCreation.setWikidataId("Q12345");
      NoteRealmWithPosition response = controller.createNote(parent, noteCreation);
      assertThat(response.noteRealm.getNote().getWikidataId(), equalTo("Q12345"));
    }

    @Test
    void shouldBeAbleToSaveNoteWithoutWikidataIdWhenValid()
        throws NoAccessRightException, BindException, InterruptedException {
      NoteRealmWithPosition response = controller.createNote(parent, noteCreation);

      assertThat(response.noteRealm.getNote().getWikidataId(), equalTo(null));
    }

    @Test
    void shouldThrowWhenCreatingNoteWithWikidataIdExistsInAnotherNote() {
      String conflictingWikidataId = "Q123";
      makeMe.aNote().under(parent).wikidataId(conflictingWikidataId).please();
      noteCreation.setWikidataId(conflictingWikidataId);
      BindException bindException =
          assertThrows(BindException.class, () -> controller.createNote(parent, noteCreation));
      assertThat(
          bindException.getMessage(), stringContainsInOrder("Duplicate Wikidata ID Detected."));
    }

    @Nested
    class AddingNoteWithLocationWikidataId {
      String wikidataIdOfALocation = "Q334";
      String lnglat = "1.3'N, 103.8'E";
      String singapore = "Singapore";

      @BeforeEach
      void thereIsAWikidataEntryOfALocation() {
        noteCreation.setWikidataId(wikidataIdOfALocation);
        noteCreation.getTextContent().setDescription(singapore);
      }

      private void mockApiResponseWithLocationInfo(String locationInfo)
          throws IOException, InterruptedException {
        Mockito.when(
                httpClientAdapter.getResponseString(
                    URI.create(
                        "https://www.wikidata.org/w/api.php?action=wbgetentities&ids="
                            + wikidataIdOfALocation
                            + "&format=json&props=claims")))
            .thenReturn(
                "{\"entities\":{\"Q334\":{\"type\":\"item\",\"id\":\"Q334\",\"claims\":{\"P625\":[{\"mainsnak\":{\"snaktype\":\"value\",\"property\":\"P625\",\"datavalue\":{"
                    + locationInfo
                    + "}}}]}}}}");
      }

      @Test
      void shouldAddLocationInfoWhenAddingNoteWithWikidataId()
          throws BindException, InterruptedException, NoAccessRightException, IOException {
        mockApiResponseWithLocationInfo(
            "\"value\":{\"latitude\":1.3,\"longitude\":103.8},\"type\":\"globecoordinate\"");
        NoteRealmWithPosition note = controller.createNote(parent, noteCreation);
        assertThat(
            note.noteRealm.getNote().getTextContent().getDescription(),
            stringContainsInOrder("Location: " + lnglat));
      }

      @Test
      void shouldPrependLocationInfoWhenAddingNoteWithWikidataId()
          throws BindException, InterruptedException, NoAccessRightException, IOException {
        mockApiResponseWithLocationInfo(
            "\"value\":{\"latitude\":1.3,\"longitude\":103.8},\"type\":\"globecoordinate\"");
        NoteRealmWithPosition note = controller.createNote(parent, noteCreation);
        assertThat(
            note.noteRealm.getNote().getTextContent().getDescription(),
            stringContainsInOrder("Location: " + lnglat, singapore));
      }

      @Test
      void shouldPrependLocationInfoWhenAddingNoteWithWikidataIdWithStringValue()
          throws BindException, InterruptedException, NoAccessRightException, IOException {
        mockApiResponseWithLocationInfo("\"value\": \"center of the earth\",\"type\":\"string\"");
        NoteRealmWithPosition note = controller.createNote(parent, noteCreation);
        assertThat(
            note.noteRealm.getNote().getTextContent().getDescription(),
            stringContainsInOrder("Location: center of the earth"));
      }
    }

    @Nested
    class AddingNoteWithHumanWikidataId {
      @BeforeEach
      void thereIsAWikidataEntryOfAHuman() {
        noteCreation.setWikidataId("");
        noteCreation.getTextContent().setDescription("");
      }

      private void mockApiResponseWithHumanInfo(
          String humanId, String countryInfo, String birthdayInfo)
          throws IOException, InterruptedException {
        Mockito.when(
                httpClientAdapter.getResponseString(
                    URI.create(
                        "https://www.wikidata.org/w/api.php?action=wbgetentities&ids="
                            + humanId
                            + "&format=json&props=claims")))
            .thenReturn(
                "{\"entities\":{\""
                    + humanId
                    + "\":{\"type\":\"item\",\"id\":\""
                    + humanId
                    + "\",\"claims\":{"
                    + "\"P27\":[{\"mainsnak\":{\"snaktype\":\"value\",\"property\":\"P27\",\"datavalue\":{"
                    + countryInfo
                    + "}}}],"
                    + "\"P569\":[{\"mainsnak\":{\"snaktype\":\"value\",\"property\":\"P569\",\"datavalue\":{"
                    + birthdayInfo
                    + "}}}]"
                    + "}}}}");
      }

      private void mockApiResponseWithCountryInfo(
          String wikidataIdOfCountry, String wikidataCountryName)
          throws IOException, InterruptedException {
        Mockito.when(
                httpClientAdapter.getResponseString(
                    URI.create(
                        "https://www.wikidata.org/w/api.php?action=wbgetentities&ids="
                            + wikidataIdOfCountry
                            + "&format=json&props=labels")))
            .thenReturn(
                "{\"entities\":{\""
                    + wikidataIdOfCountry
                    + "\":{\"type\":\"item\",\"id\":\""
                    + wikidataIdOfCountry
                    + "\",\"labels\":{"
                    + "{\"en\":{\"language\":\"en\",\"value\":\"Taiwan\"}}"
                    + "}}}}");
      }

      @Test
      void shouldAddHumanInfoWhenAddingNoteWithWikidataId()
          throws BindException, InterruptedException, NoAccessRightException, IOException {
        String wikidataIdOfHuman = "Q706446"; // Wang Chien-ming
        String wikidataIdOfCountry = "Q865"; // P27 (Country) : Taiwan
        String wikidataCountryName = "Taiwan";
        String birthday = "31 March 1980"; // P569 (Birthday)
        String birthdayByISO = "+1980-03-31T00:00:00Z";

        mockApiResponseWithHumanInfo(
            wikidataIdOfHuman,
            "\"value\":{\"id\":\"Q865\"},\"type\":\"wikibase-entityid\"",
            "\"value\":{\"time\":\"" + birthdayByISO + "\"},\"type\":\"time\"");
        mockApiResponseWithCountryInfo(wikidataIdOfCountry, wikidataCountryName);
        noteCreation.setWikidataId(wikidataIdOfHuman);
        NoteRealmWithPosition note = controller.createNote(parent, noteCreation);
        assertThat(
            note.noteRealm.getNote().getTextContent().getDescription(),
            stringContainsInOrder(birthday + ", " + wikidataCountryName));
      }
    }
  }

  @Nested
  class updateNoteTest {
    Note note;

    @BeforeEach
    void setup() {
      note = makeMe.aNote("new").creatorAndOwner(userModel).please();
    }

    @Test
    void shouldBeAbleToSaveNoteWhenValid() throws NoAccessRightException, IOException {
      NoteRealm response = controller.updateNote(note, note.getNoteAccessories());
      assertThat(response.getId(), equalTo(note.getId()));
    }

    @Test
    void shouldAddUploadedPicture() throws NoAccessRightException, IOException {
      makeMe.theNote(note).withNewlyUploadedPicture();
      controller.updateNote(note, note.getNoteAccessories());
      assertThat(note.getNoteAccessories().getUploadPicture(), is(not(nullValue())));
    }

    @Test
    void shouldNotRemoveThePictureIfNoNewPictureInTheUpdate()
        throws NoAccessRightException, IOException {
      makeMe.theNote(note).withUploadedPicture();
      NoteAccessories newContent = makeMe.aNote().inMemoryPlease().getNoteAccessories();
      controller.updateNote(note, newContent);
      assertThat(note.getNoteAccessories().getUploadPicture(), is(not(nullValue())));
    }
  }

  @Nested
  class DeleteNoteTest {
    Note subject;
    Note parent;
    Note child;

    @BeforeEach
    void setup() {
      parent = makeMe.aNote().creatorAndOwner(userModel).please();
      subject = makeMe.aNote().under(parent).please();
      child = makeMe.aNote("child").under(subject).please();
      makeMe.refresh(subject);
    }

    @Test
    void shouldNotBeAbleToDeleteNoteThatBelongsToOtherUser() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(anotherUser).please();
      assertThrows(NoAccessRightException.class, () -> controller.deleteNote(note));
    }

    @Test
    void shouldDeleteTheNoteButNotTheUser() throws NoAccessRightException {
      controller.deleteNote(subject);
      makeMe.refresh(parent);
      assertThat(parent.getChildren(), hasSize(0));
      assertTrue(modelFactoryService.findUserById(userModel.getEntity().getId()).isPresent());
    }

    @Test
    void shouldDeleteTheChildNoteButNotSibling() throws NoAccessRightException {
      makeMe.aNote("silbling").under(parent).please();
      controller.deleteNote(subject);
      makeMe.refresh(parent);
      assertThat(parent.getChildren(), hasSize(1));
      assertThat(parent.getDescendantsInBreathFirstOrder(), hasSize(1));
    }

    @Nested
    class UndoDeleteNoteTest {
      @Test
      void shouldUndoDeleteTheNote() throws NoAccessRightException {
        controller.deleteNote(subject);
        makeMe.refresh(subject);
        controller.undoDeleteNote(subject);
        makeMe.refresh(parent);
        assertThat(parent.getChildren(), hasSize(1));
        assertThat(parent.getDescendantsInBreathFirstOrder(), hasSize(2));
      }

      @Test
      void shouldUndoOnlylastChange() throws NoAccessRightException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        testabilitySettings.timeTravelTo(timestamp);
        controller.deleteNote(child);
        makeMe.refresh(subject);

        timestamp = TimestampOperations.addHoursToTimestamp(timestamp, 1);
        testabilitySettings.timeTravelTo(timestamp);
        controller.deleteNote(subject);
        makeMe.refresh(subject);

        controller.undoDeleteNote(subject);
        makeMe.refresh(parent);
        assertThat(parent.getDescendantsInBreathFirstOrder(), hasSize(1));
      }
    }
  }

  @Nested
  class gettingPosition {}

  @Test
  void shouldNotBeAbleToAddCommentToNoteTheUserCannotSee() {
    User anotherUser = makeMe.aUser().please();
    Note note = makeMe.aNote().creatorAndOwner(anotherUser).please();
    assertThrows(NoAccessRightException.class, () -> controller.getPosition(note));
  }

  @Nested
  class UpdateWikidataId {
    Note note;
    Note parent;
    String noteWikidataId = "Q1234";

    @BeforeEach
    void setup() {
      parent = makeMe.aNote().creatorAndOwner(userModel).please();
      note = makeMe.aNote().under(parent).please();
    }

    @Test
    void shouldUpdateNoteWithUniqueWikidataId() throws BindException, NoAccessRightException {
      WikidataAssociationCreation wikidataAssociationCreation = new WikidataAssociationCreation();
      wikidataAssociationCreation.wikidataId = "Q123";
      controller.updateWikidataId(note, wikidataAssociationCreation);
      Note sameNote = makeMe.modelFactoryService.noteRepository.findById(note.getId()).get();
      assertThat(sameNote.getWikidataId(), equalTo("Q123"));
    }

    @Test
    void shouldNotUpdateWikidataIdIfParentNoteSameWikidataId() {
      makeMe.aNote().under(parent).wikidataId(noteWikidataId).please();

      WikidataAssociationCreation wikidataAssociationCreation = new WikidataAssociationCreation();
      wikidataAssociationCreation.wikidataId = noteWikidataId;
      BindException bindException =
          assertThrows(
              BindException.class,
              () -> controller.updateWikidataId(note, wikidataAssociationCreation));
      assertThat(
          bindException.getMessage(), stringContainsInOrder("Duplicate Wikidata ID Detected."));
    }
  }
}
