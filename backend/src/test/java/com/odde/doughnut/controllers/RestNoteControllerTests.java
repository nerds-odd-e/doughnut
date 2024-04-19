package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.LinkType;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.TimestampOperations;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.MakeMeWithoutDB;
import com.odde.doughnut.testability.TestabilitySettings;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.util.List;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestNoteControllerTests {
  @Autowired ModelFactoryService modelFactoryService;

  @Autowired MakeMe makeMe;
  @Mock HttpClientAdapter httpClientAdapter;
  @Mock RestTemplate restTemplate;
  private UserModel userModel;
  RestNoteController controller;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();

    controller =
        new RestNoteController(
            modelFactoryService, userModel, httpClientAdapter, testabilitySettings, restTemplate);
  }

  private void mockWikidataEntity(String wikidataId, String label)
      throws IOException, InterruptedException {
    if (Strings.isEmpty(wikidataId) || Strings.isEmpty(label)) {
      return;
    }
    Mockito.when(
            httpClientAdapter.getResponseString(
                URI.create(
                    "https://www.wikidata.org/wiki/Special:EntityData/" + wikidataId + ".json")))
        .thenReturn(makeMe.wikidataClaimsJson(wikidataId).labelIf(label).please());
  }

  private void mockWikidataWBGetEntity(String personWikidataId, String value)
      throws IOException, InterruptedException {
    Mockito.when(
            httpClientAdapter.getResponseString(
                URI.create(
                    "https://www.wikidata.org/w/api.php?action=wbgetentities&ids="
                        + personWikidataId
                        + "&format=json&props=claims")))
        .thenReturn(value);
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
      final NoteRealm noteRealm = controller.show(note);
      assertThat(noteRealm.getNote().getTopicConstructor(), equalTo(note.getTopicConstructor()));
      assertThat(noteRealm.getNotePosition().getFromBazaar(), is(true));
    }

    @Test
    void shouldBeAbleToSeeOwnNote() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(userModel).please();
      makeMe.refresh(userModel.getEntity());
      final NoteRealm noteRealm = controller.show(note);
      assertThat(noteRealm.getId(), equalTo(note.getId()));
      assertThat(noteRealm.getNotePosition().getFromBazaar(), is(false));
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
      makeMe.flush();
      makeMe.refresh(userModel.getEntity());
      assertThat(controller.getNoteInfo(note).getNote().getId(), equalTo(note.getId()));
    }
  }

  @Nested
  class createNoteTest {
    Note parent;
    NoteCreationDTO noteCreation = new NoteCreationDTO();

    @BeforeEach
    void setup() {
      parent = makeMe.aNote().creatorAndOwner(userModel).please();
      noteCreation.setTopicConstructor("new title");
      noteCreation.setLinkTypeToParent(LinkType.NO_LINK);
    }

    @Test
    void shouldBeAbleToSaveNoteWhenValid()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      NoteRealm response = controller.createNote(parent, noteCreation);
      assertThat(response.getId(), not(nullValue()));
    }

    @Test
    void shouldBeAbleToCreateAThing()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      long beforeThingCount = makeMe.modelFactoryService.noteRepository.count();
      controller.createNote(parent, noteCreation);
      long afterThingCount = makeMe.modelFactoryService.noteRepository.count();
      assertThat(afterThingCount, equalTo(beforeThingCount + 1));
    }

    @Test
    void shouldBeAbleToSaveNoteWithWikidataIdWhenValid()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(new MakeMeWithoutDB().wikidataEntityJson().entityId("Q12345").please());
      noteCreation.setWikidataId("Q12345");
      NoteRealm response = controller.createNote(parent, noteCreation);
      assertThat(response.getNote().getWikidataId(), equalTo("Q12345"));
    }

    @Test
    void shouldBeAbleToSaveNoteWithoutWikidataIdWhenValid()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      NoteRealm response = controller.createNote(parent, noteCreation);

      assertThat(response.getNote().getWikidataId(), equalTo(null));
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
      }

      private void mockApiResponseWithLocationInfo(String locationInfo, String type)
          throws IOException, InterruptedException {
        mockWikidataWBGetEntity(
            wikidataIdOfALocation,
            makeMe.wikidataClaimsJson("Q334").globeCoordinate(locationInfo, type).please());
      }

      @Test
      void shouldPrependLocationInfoWhenAddingNoteWithWikidataId()
          throws BindException,
              InterruptedException,
              UnexpectedNoAccessRightException,
              IOException {
        mockApiResponseWithLocationInfo(
            "{\"latitude\":1.3,\"longitude\":103.8}", "globecoordinate");
        NoteRealm note = controller.createNote(parent, noteCreation);
        assertThat(note.getNote().getDetails(), containsString("Location: " + lnglat));
      }

      @Test
      void shouldPrependLocationInfoWhenAddingNoteWithWikidataIdWithStringValue()
          throws BindException,
              InterruptedException,
              UnexpectedNoAccessRightException,
              IOException {
        mockApiResponseWithLocationInfo("\"center of the earth\"", "string");
        NoteRealm note = controller.createNote(parent, noteCreation);
        assertThat(
            note.getNote().getDetails(), stringContainsInOrder("Location: center of the earth"));
      }
    }

    @Nested
    class AddingNoteWithHumanWikidataId {
      @BeforeEach
      void thereIsAWikidataEntryOfAHuman() {
        noteCreation.setWikidataId("");
      }

      private void mockWikidataHumanEntity(
          String personWikidataId, String birthdayByISO, String countryQId)
          throws IOException, InterruptedException {
        mockWikidataWBGetEntity(
            personWikidataId,
            makeMe
                .wikidataClaimsJson(personWikidataId)
                .asAHuman()
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
          throws BindException,
              InterruptedException,
              UnexpectedNoAccessRightException,
              IOException {
        mockWikidataHumanEntity(wikidataIdOfHuman, birthdayByISO, countryQid);
        mockWikidataEntity(countryQid, countryName);
        noteCreation.setWikidataId(wikidataIdOfHuman);
        NoteRealm note = controller.createNote(parent, noteCreation);
        String description = note.getNote().getDetails();
        if (expectedBirthday != null) {
          assertThat(description, containsString(expectedBirthday));
        }
        if (countryName != null) {
          assertThat(description, containsString(countryName));
        }
      }

      @Test
      void shouldAddPersonNoteWithCountryNoteWithWikidataId()
          throws BindException,
              InterruptedException,
              UnexpectedNoAccessRightException,
              IOException {

        mockWikidataHumanEntity("Q8337", null, "Q34660");
        mockWikidataEntity("Q34660", "Canada");
        noteCreation.setWikidataId("Q8337");
        noteCreation.setTopicConstructor("Johnny boy");
        NoteRealm note = controller.createNote(parent, noteCreation);
        makeMe.refresh(note.getNote());

        assertEquals("Johnny boy", note.getNote().getTopicConstructor());
        assertEquals("Q8337", note.getNote().getWikidataId());
        assertEquals("Canada", note.getNote().getChildren().get(0).getTopicConstructor());
      }
    }

    @Nested
    class AddingBookNoteWithAuthorInformation {
      @BeforeEach
      void setup() throws IOException, InterruptedException {
        mockWikidataEntity("Q34660", "J. K. Rowling");
        mockWikidataEntity("Q12345", "The girl sat next to the window");
        noteCreation.setWikidataId("Q8337");
        noteCreation.setTopicConstructor("Harry Potter");
      }

      @Test
      void shouldAddBookNoteWithAuthorNoteWithWikidataId()
          throws BindException,
              InterruptedException,
              UnexpectedNoAccessRightException,
              IOException {
        mockWikidataWBGetEntity(
            "Q8337", makeMe.wikidataClaimsJson("Q8337").asABookWithSingleAuthor("Q34660").please());
        NoteRealm note = controller.createNote(parent, noteCreation);
        makeMe.refresh(note.getNote());

        assertEquals("Harry Potter", note.getNote().getTopicConstructor());
        assertEquals("Q8337", note.getNote().getWikidataId());
        assertEquals("J. K. Rowling", note.getNote().getChildren().get(0).getTopicConstructor());
      }

      @Test
      void shouldAddBookNoteWithMultipleAuthorsNoteWithWikidataId()
          throws BindException,
              InterruptedException,
              UnexpectedNoAccessRightException,
              IOException {
        mockWikidataWBGetEntity(
            "Q8337",
            makeMe
                .wikidataClaimsJson("Q8337")
                .asABookWithMultipleAuthors(List.of("Q34660", "Q12345"))
                .please());
        NoteRealm note = controller.createNote(parent, noteCreation);
        makeMe.refresh(note.getNote());

        assertEquals(
            "The girl sat next to the window",
            note.getNote().getChildren().get(1).getTopicConstructor());
      }
    }
  }

  @Nested
  class updateNoteTest {
    Note note;
    NoteAccessoriesDTO noteAccessoriesDTO = new NoteAccessoriesDTO();
    AudioUploadDTO audioUploadDTO = new AudioUploadDTO();

    @BeforeEach
    void setup() {
      note = makeMe.aNote("new").creatorAndOwner(userModel).please();
    }

    @Test
    void shouldBeAbleToSaveNoteWhenValid() throws UnexpectedNoAccessRightException, IOException {
      NoteRealm response = controller.updateNoteAccessories(note, noteAccessoriesDTO);
      assertThat(response.getId(), equalTo(note.getId()));
    }

    @Test
    void shouldAddUploadedPicture() throws UnexpectedNoAccessRightException, IOException {
      noteAccessoriesDTO.setUploadPictureProxy(
          makeMe.anUploadedPicture().toMultiplePartFilePlease());
      controller.updateNoteAccessories(note, noteAccessoriesDTO);
      assertThat(note.getNoteAccessories().getUploadPicture(), is(not(nullValue())));
      note.getNoteAccessories().getUploadPicture().getImageBlob().getData();
    }

    @Test
    void shouldSaveTheBlogData() throws UnexpectedNoAccessRightException, IOException {
      noteAccessoriesDTO.setUploadPictureProxy(
          makeMe.anUploadedPicture().toMultiplePartFilePlease());
      controller.updateNoteAccessories(note, noteAccessoriesDTO);
      byte[] data = note.getNoteAccessories().getUploadPicture().getImageBlob().getData();
      assertThat(data.length, is(68));
    }

    @Test
    void shouldNotRemoveThePictureIfNoNewPictureInTheUpdate()
        throws UnexpectedNoAccessRightException, IOException {
      makeMe.theNote(note).withUploadedPicture();
      controller.updateNoteAccessories(note, noteAccessoriesDTO);
      assertThat(note.getNoteAccessories().getUploadPicture(), is(not(nullValue())));
    }

    @ParameterizedTest
    @ValueSource(strings = {"podcast.mp3", "podcast.m4a", "podcast.wav"})
    void shouldSucceedOnValidAudioFileFormat(String filename) throws Exception {
      audioUploadDTO.setUploadAudioFile(
          new MockMultipartFile(filename, filename, "audio/mp3", new byte[] {}));
      NoteRealm noteRealm = controller.upload(note, audioUploadDTO, false);
      assertEquals(
          noteRealm.getNote().getNoteAccessories().getAudioName().get(),
          audioUploadDTO.getUploadAudioFile().getOriginalFilename());
    }

    @ParameterizedTest
    @ValueSource(strings = {"something.txt", "youtube.avi"})
    void shouldFailOnInvalidAudioFileFormat(String filename)
        throws UnexpectedNoAccessRightException, IOException {
      byte[] bytes = new byte[1024 * 1024 * 10];
      audioUploadDTO.setUploadAudioFile(
          new MockMultipartFile(filename, filename, "audio/mp3", new byte[] {}));

      assertThrows(
          Exception.class,
          () -> {
            controller.upload(note, audioUploadDTO, false);
          });
    }

    @Test
    void shouldFailOnFileSize() throws UnexpectedNoAccessRightException, IOException {
      String filename = "big_file.mp3";
      byte[] bytes = new byte[1024 * 1024 * 20];
      audioUploadDTO.setUploadAudioFile(
          new MockMultipartFile(filename, filename, "audio/mp3", bytes));

      assertThrows(
          Exception.class,
          () -> {
            controller.upload(note, audioUploadDTO, false);
          });
    }

    @Test
    void shouldPersistAudioToNoteAfterUpload() throws Exception {
      String filename = "podcast.wav";
      audioUploadDTO.setUploadAudioFile(
          new MockMultipartFile(filename, filename, "audio/wav", new byte[] {}));
      controller.upload(note, audioUploadDTO, false);
      Note newNote = makeMe.modelFactoryService.noteRepository.findById(note.getId()).get();
      assertEquals(filename, newNote.getNoteAccessories().getUploadAudio().getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"podcast.mp3", "podcast.m4a", "podcast.wav"})
    void isUoloadingAndConverting(String filename) throws Exception {
      audioUploadDTO.setUploadAudioFile(
          new MockMultipartFile(filename, filename, "audio/mp3", new byte[] {}));
      ResponseEntity<String> mockResponseEntity = new ResponseEntity<>("test", HttpStatus.OK);
      when(restTemplate.exchange(
              any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
          .thenReturn(mockResponseEntity);
      NoteRealm noteRealm = controller.upload(note, audioUploadDTO, true);
      assertEquals(noteRealm.getNote().getSrt(), "test");
    }

    @Test
    void convertAudioToSRT() {
      MockMultipartFile mockFile =
          new MockMultipartFile("file", "test.mp3", "text/plain", "test".getBytes());
      var dto = new AudioUploadDTO();
      dto.setUploadAudioFile(mockFile);
      // Mocking the response entity
      ResponseEntity<String> mockResponseEntity = new ResponseEntity<>("test", HttpStatus.OK);
      when(restTemplate.exchange(
              any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
          .thenReturn(mockResponseEntity);

      String resp = controller.convertSrt(dto);

      assertThat(resp, equalTo("test"));
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
      assertThat(parent.getDescendants().toList(), hasSize(1));
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
        assertThat(parent.getDescendants().toList(), hasSize(2));
      }

      @Test
      void shouldUndoOnlyLastChange() throws UnexpectedNoAccessRightException {
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
        assertThat(parent.getDescendants().toList(), hasSize(1));
      }
    }
  }

  @Nested
  class gettingPosition {
    @Test
    void shouldNotBeAbleToAddCommentToNoteTheUserCannotSee() {
      User anotherUser = makeMe.aUser().please();
      Note note = makeMe.aNote().creatorAndOwner(anotherUser).please();
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.getPosition(note));
    }
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
        throws BindException, UnexpectedNoAccessRightException, IOException, InterruptedException {
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

  @Nested
  class UpdateReviewSetting {
    Note source;
    Note target;
    Note link;

    @BeforeEach
    void setup() {
      source = makeMe.aNote().creatorAndOwner(userModel).please();
      target = makeMe.aNote().creatorAndOwner(userModel).please();
      link = makeMe.aLink().between(source, target).please();
    }

    @Test
    void shouldUpdateLinkLevel() throws UnexpectedNoAccessRightException {
      @Valid ReviewSetting reviewSetting = new ReviewSetting();
      reviewSetting.setLevel(4);
      controller.updateReviewSetting(source, reviewSetting);
      makeMe.refresh(link);
      assertThat(getLevel(link), is(4));
    }

    @Test
    void shouldUpdateReferenceLevel() throws UnexpectedNoAccessRightException {
      @Valid ReviewSetting reviewSetting = new ReviewSetting();
      reviewSetting.setLevel(4);
      controller.updateReviewSetting(target, reviewSetting);
      makeMe.refresh(link);
      assertThat(getLevel(link), is(4));
    }

    private static Integer getLevel(Note link) {
      return link.getReviewSetting().getLevel();
    }
  }
}
