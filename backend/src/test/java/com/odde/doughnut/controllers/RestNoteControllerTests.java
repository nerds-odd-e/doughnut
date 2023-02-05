package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import com.odde.doughnut.entities.Link.LinkType;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteAccessories;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.json.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.TimestampOperations;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.HttpClientAdapter;
import com.odde.doughnut.services.OpenAiWrapperService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
  @Mock OpenAiWrapperService openAiWrapperService;
  private UserModel userModel;
  RestNoteController controller;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();

    controller =
        new RestNoteController(
            modelFactoryService,
            userModel,
            httpClientAdapter,
            testabilitySettings,
            openAiWrapperService);
  }

  @Nested
  class showNoteTest {
    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      User otherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(otherUser).please();
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.show(note));
    }

    @Test
    void shouldReturnTheNoteInfoIfHavingReadingAuth() throws UnexpectedNoAccessRightException {
      User otherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(otherUser).please();
      makeMe.aBazaarNodebook(note.getNotebook()).please();
      makeMe.refresh(userModel.getEntity());
      final NoteRealmWithPosition show = controller.show(note);
      assertThat(show.noteRealm.getNote().getTitle(), equalTo(note.getTitle()));
      assertThat(show.notePosition.getNotebook().getFromBazaar(), is(true));
    }

    @Test
    void shouldBeAbleToSeeOwnNote() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(userModel).please();
      makeMe.refresh(userModel.getEntity());
      final NoteRealmWithPosition show = controller.show(note);
      assertThat(show.noteRealm.getId(), equalTo(note.getId()));
      assertThat(show.notePosition.getNotebook().getFromBazaar(), is(false));
    }

    @Test
    void shouldBeAbleToSeeOwnNoteOverview() throws UnexpectedNoAccessRightException {
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
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.getNoteInfo(note));
    }

    @Test
    void shouldReturnTheNoteInfoIfHavingReadingAuth() throws UnexpectedNoAccessRightException {
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
    void shouldBeAbleToSaveNoteWithAiDescription()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException {
      noteCreation.getTextContent().setDescription("");
      String expectedDescription = "This is a description from OpenAi";
      Mockito.when(openAiWrapperService.getDescription(any())).thenReturn(expectedDescription);
      NoteRealmWithPosition response = controller.createNote(parent, noteCreation);
      assertThat(response.noteRealm.getNote().getShortDescription(), equalTo(expectedDescription));
    }

    @Test
    void shouldBeAbleToSaveNoteWhenValid()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException {
      NoteRealmWithPosition response = controller.createNote(parent, noteCreation);
      assertThat(response.noteRealm.getId(), not(nullValue()));
    }

    @Test
    void shouldBeAbleToCreateAThing()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException {
      long beforeThingCount = makeMe.modelFactoryService.thingRepository.count();
      controller.createNote(parent, noteCreation);
      long afterThingCount = makeMe.modelFactoryService.thingRepository.count();
      assertThat(afterThingCount, equalTo(beforeThingCount + 1));
    }

    @Test
    void shouldBeAbleToSaveNoteWithWikidataIdWhenValid()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(new MakeMe().wikidataEntityJson().entityId("Q12345").please());
      noteCreation.setWikidataId("Q12345");
      NoteRealmWithPosition response = controller.createNote(parent, noteCreation);
      assertThat(response.noteRealm.getNote().getWikidataId(), equalTo("Q12345"));
    }

    @Test
    void shouldBeAbleToSaveNoteWithoutWikidataIdWhenValid()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException {
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

      private void mockApiResponseWithLocationInfo(String locationInfo, String type)
          throws IOException, InterruptedException {
        Mockito.when(
                httpClientAdapter.getResponseString(
                    URI.create(
                        "https://www.wikidata.org/w/api.php?action=wbgetentities&ids="
                            + wikidataIdOfALocation
                            + "&format=json&props=claims")))
            .thenReturn(
                makeMe.wikidataClaimsJson("Q334").globeCoordinate(locationInfo, type).please());
      }

      @Test
      void shouldPrependLocationInfoWhenAddingNoteWithWikidataId()
          throws BindException, InterruptedException, UnexpectedNoAccessRightException,
              IOException {
        mockApiResponseWithLocationInfo(
            "{\"latitude\":1.3,\"longitude\":103.8}", "globecoordinate");
        NoteRealmWithPosition note = controller.createNote(parent, noteCreation);
        assertThat(
            note.noteRealm.getNote().getTextContent().getDescription(),
            stringContainsInOrder("Location: " + lnglat, singapore));
      }

      @Test
      void shouldAddCoordinatesWhenAddingLocationNoteWithWikidataId() throws Exception {
        mockApiResponseWithLocationInfo(
            "{\"latitude\":1.3,\"longitude\":103.8}", "globecoordinate");

        var note = controller.createNote(parent, noteCreation);

        assertThat(note.noteRealm.getNote().getLocation().getLatitude(), equalTo(1.3));
        assertThat(note.noteRealm.getNote().getLocation().getLongitude(), equalTo(103.8));
      }

      @Test
      void shouldPrependLocationInfoWhenAddingNoteWithWikidataIdWithStringValue()
          throws BindException, InterruptedException, UnexpectedNoAccessRightException,
              IOException {
        mockApiResponseWithLocationInfo("\"center of the earth\"", "string");
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

      private void mockWikidataCountryEntity(String countryQId, String countryName)
          throws IOException, InterruptedException {
        if (Strings.isEmpty(countryQId) || Strings.isEmpty(countryName)) {
          return;
        }
        Mockito.when(
                httpClientAdapter.getResponseString(
                    URI.create(
                        "https://www.wikidata.org/wiki/Special:EntityData/"
                            + countryQId
                            + ".json")))
            .thenReturn(makeMe.wikidataClaimsJson(countryQId).labelIf(countryName).please());
      }

      private void mockWikidataHumanEntity(
          String personWikidataId, String birthdayByISO, String countryQId)
          throws IOException, InterruptedException {
        Mockito.when(
                httpClientAdapter.getResponseString(
                    URI.create(
                        "https://www.wikidata.org/w/api.php?action=wbgetentities&ids="
                            + personWikidataId
                            + "&format=json&props=claims")))
            .thenReturn(
                makeMe
                    .wikidataClaimsJson(personWikidataId)
                    .asHuman()
                    .countryOfOrigin(countryQId)
                    .birthdayIf(birthdayByISO)
                    .please());
      }

      @ParameterizedTest
      @CsvSource(
          useHeadersInDisplayName = true,
          delimiter = '|',
          textBlock =
              """
             WikidataId | Birthday from Wikidata | CountryQID | Country Name | Expected Birthday    | Name
            #---------------------------------------------------------------------------------------------
             Q706446    | +1980-03-31T00:00:00Z  |            |              | 31 March 1980        |
             Q4604      | -0552-10-09T00:00:00Z  | Q736936    |              | 09 October 0552 B.C. | Confucius
             Q706446    | +1980-03-31T00:00:00Z  | Q865       | Taiwan       | 31 March 1980        | Wang Chen-ming
             Q706446    |                        | Q865       | Taiwan       |                      |
             Q706446    | +1980-03-31T00:00:00Z  | Q30        | The US of A  |  31 March 1980       |
             """)
      void shouldAddHumanBirthdayAndCountryOfOriginWhenAddingNoteWithWikidataId(
          String wikidataIdOfHuman,
          String birthdayByISO,
          String countryQid,
          String countryName,
          String expectedBirthday)
          throws BindException, InterruptedException, UnexpectedNoAccessRightException,
              IOException {
        mockWikidataHumanEntity(wikidataIdOfHuman, birthdayByISO, countryQid);
        mockWikidataCountryEntity(countryQid, countryName);
        noteCreation.setWikidataId(wikidataIdOfHuman);
        NoteRealmWithPosition note = controller.createNote(parent, noteCreation);
        String description = note.noteRealm.getNote().getTextContent().getDescription();
        if (expectedBirthday != null) {
          assertThat(description, containsString(expectedBirthday));
        }
        if (countryName != null) {
          assertThat(description, containsString(countryName));
        }
      }

      @Test
      void shouldAddPersonNoteWithCountryNoteWithWikidataId()
          throws BindException, InterruptedException, UnexpectedNoAccessRightException,
              IOException {

        mockWikidataHumanEntity("Q8337", null, "Q34660");
        mockWikidataCountryEntity("Q34660", "Canada");
        noteCreation.setWikidataId("Q8337");
        noteCreation.getTextContent().setTitle("Johnny boy");
        NoteRealmWithPosition note = controller.createNote(parent, noteCreation);
        makeMe.refresh(note.noteRealm.getNote());

        assertEquals("Johnny boy", note.noteRealm.getNote().getTitle());
        assertEquals("Q8337", note.noteRealm.getNote().getWikidataId());
        assertEquals("Canada", note.noteRealm.getNote().getChildren().get(0).getTitle());
      }
    }

    @Nested
    class AddingBookNoteWithAuthorInformation {
      private void mockApiResponseForBookWithAuthorTag(
          String bookWikiDataId, String bookAuthorWikiDataId)
          throws IOException, InterruptedException {
        Mockito.when(
                httpClientAdapter.getResponseString(
                    URI.create(
                        "https://www.wikidata.org/w/api.php?action=wbgetentities&ids="
                            + bookWikiDataId
                            + "&format=json&props=claims")))
            .thenReturn(
                makeMe.wikidataClaimsJson(bookWikiDataId).asBook(bookAuthorWikiDataId).please());
      }

      private void mockApiResponseForAuthor(String bookAuthorWikiDataId, String authorName)
          throws IOException, InterruptedException {
        Mockito.when(
                httpClientAdapter.getResponseString(
                    URI.create(
                        "https://www.wikidata.org/wiki/Special:EntityData/"
                            + bookAuthorWikiDataId
                            + ".json")))
            .thenReturn(
                makeMe.wikidataClaimsJson(bookAuthorWikiDataId).labelIf(authorName).please());
      }

      @ParameterizedTest
      @CsvSource(
          useHeadersInDisplayName = true,
          delimiter = '|',
          textBlock =
              """
    bookWikiDataId | bookName | AuthorWikiDataId | AuthorWikiDataName
   #---------------------------------------------------------------------------------------------
    Q8337    | Harry Potter  | Q34660           |  J. K. Rowling
    """)
      void shouldAddBookNoteWithAuthorNoteWithWikidataId(
          String wikidataIdOfBook,
          String bookName,
          String authorWikiDataId,
          String authorWikiDataName)
          throws BindException, InterruptedException, UnexpectedNoAccessRightException,
              IOException {
        mockApiResponseForBookWithAuthorTag(wikidataIdOfBook, authorWikiDataId);
        mockApiResponseForAuthor(authorWikiDataId, authorWikiDataName);
        noteCreation.setWikidataId(wikidataIdOfBook);
        noteCreation.getTextContent().setTitle(bookName);
        NoteRealmWithPosition note = controller.createNote(parent, noteCreation);
        makeMe.refresh(note.noteRealm.getNote());

        String actualBookNoteWikiDataId = note.noteRealm.getNote().getWikidataId();
        String actualAuthorNoteTitle = note.noteRealm.getNote().getChildren().get(0).getTitle();
        String actualBookNoteTitle = note.noteRealm.getNote().getTitle();

        assertEquals(bookName, actualBookNoteTitle);
        assertEquals(wikidataIdOfBook, actualBookNoteWikiDataId);
        assertEquals(authorWikiDataName, actualAuthorNoteTitle);
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
    void shouldBeAbleToSaveNoteWhenValid() throws UnexpectedNoAccessRightException, IOException {
      NoteRealm response = controller.updateNote(note, note.getNoteAccessories());
      assertThat(response.getId(), equalTo(note.getId()));
    }

    @Test
    void shouldAddUploadedPicture() throws UnexpectedNoAccessRightException, IOException {
      makeMe.theNote(note).withNewlyUploadedPicture();
      controller.updateNote(note, note.getNoteAccessories());
      assertThat(note.getNoteAccessories().getUploadPicture(), is(not(nullValue())));
    }

    @Test
    void shouldNotRemoveThePictureIfNoNewPictureInTheUpdate()
        throws UnexpectedNoAccessRightException, IOException {
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
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.deleteNote(note));
    }

    @Test
    void shouldDeleteTheNoteButNotTheUser() throws UnexpectedNoAccessRightException {
      controller.deleteNote(subject);
      makeMe.refresh(parent);
      assertThat(parent.getChildren(), hasSize(0));
      assertTrue(modelFactoryService.findUserById(userModel.getEntity().getId()).isPresent());
    }

    @Test
    void shouldDeleteTheChildNoteButNotSibling() throws UnexpectedNoAccessRightException {
      makeMe.aNote("silbling").under(parent).please();
      controller.deleteNote(subject);
      makeMe.refresh(parent);
      assertThat(parent.getChildren(), hasSize(1));
      assertThat(parent.getDescendantsInBreathFirstOrder(), hasSize(1));
    }

    @Nested
    class UndoDeleteNoteTest {
      @Test
      void shouldUndoDeleteTheNote() throws UnexpectedNoAccessRightException {
        controller.deleteNote(subject);
        makeMe.refresh(subject);
        controller.undoDeleteNote(subject);
        makeMe.refresh(parent);
        assertThat(parent.getChildren(), hasSize(1));
        assertThat(parent.getDescendantsInBreathFirstOrder(), hasSize(2));
      }

      @Test
      void shouldUndoOnlylastChange() throws UnexpectedNoAccessRightException {
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
    assertThrows(UnexpectedNoAccessRightException.class, () -> controller.getPosition(note));
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
    void shouldUpdateNoteWithUniqueWikidataId()
        throws BindException, UnexpectedNoAccessRightException {
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
