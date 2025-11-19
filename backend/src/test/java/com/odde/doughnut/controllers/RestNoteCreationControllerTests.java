package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.NoteService;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.AuthorizationServiceTestHelper;
import com.odde.doughnut.testability.MakeMeWithoutDB;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteCreationControllerTests {
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired AuthorizationService authorizationService;
  @Autowired MakeMe makeMe;
  @Autowired NoteService noteService;
  @Mock HttpClientAdapter httpClientAdapter;
  private CurrentUser currentUser;
  NoteCreationController controller;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void setup() {
    currentUser = new CurrentUser(makeMe.aUser().please());
    controller =
        new NoteCreationController(
            modelFactoryService,
            httpClientAdapter,
            testabilitySettings,
            noteService,
            authorizationService);
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
  class createNoteTest {
    Note parent;
    NoteCreationDTO noteCreation = new NoteCreationDTO();

    @BeforeEach
    void setup() {
      parent = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      noteCreation.setNewTitle("new title");
    }

    @Test
    void shouldBeAbleToSaveNoteWhenValid()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      NoteRealm response = controller.createNoteUnderParent(parent, noteCreation).getCreated();
      assertThat(response.getId(), not(nullValue()));
    }

    @Test
    void shouldBeAbleToCreateAThing()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      long beforeThingCount = makeMe.modelFactoryService.noteRepository.count();
      controller.createNoteUnderParent(parent, noteCreation);
      long afterThingCount = makeMe.modelFactoryService.noteRepository.count();
      assertThat(afterThingCount, equalTo(beforeThingCount + 1));
    }

    @Test
    void shouldBeAbleToSaveNoteWithWikidataIdWhenValid()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(new MakeMeWithoutDB().wikidataEntityJson().entityId("Q12345").please());
      noteCreation.setWikidataId("Q12345");
      NoteRealm response = controller.createNoteUnderParent(parent, noteCreation).getCreated();
      assertThat(response.getNote().getWikidataId(), equalTo("Q12345"));
    }

    @Test
    void shouldBeAbleToSaveNoteWithoutWikidataIdWhenValid()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      NoteRealm response = controller.createNoteUnderParent(parent, noteCreation).getCreated();
      assertThat(response.getNote().getWikidataId(), equalTo(null));
    }

    @Test
    void shouldThrowWhenCreatingNoteWithWikidataIdExistsInAnotherNote() {
      String conflictingWikidataId = "Q123";
      makeMe.aNote().under(parent).wikidataId(conflictingWikidataId).please();
      noteCreation.setWikidataId(conflictingWikidataId);
      BindException bindException =
          assertThrows(
              BindException.class, () -> controller.createNoteUnderParent(parent, noteCreation));
      assertThat(
          bindException.getMessage(), stringContainsInOrder("Duplicate Wikidata ID Detected."));
    }

    @Nested
    class AddingNoteWithLocationWikidataId {
      String wikidataIdOfALocation = "Q334";
      String lnglat = "1.3'N, 103.8'E";

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
        NoteRealm note = controller.createNoteUnderParent(parent, noteCreation).getCreated();
        assertThat(note.getNote().getDetails(), containsString("Location: " + lnglat));
      }

      @Test
      void shouldPrependLocationInfoWhenAddingNoteWithWikidataIdWithStringValue()
          throws BindException,
              InterruptedException,
              UnexpectedNoAccessRightException,
              IOException {
        mockApiResponseWithLocationInfo("\"center of the earth\"", "string");
        NoteRealm note = controller.createNoteUnderParent(parent, noteCreation).getCreated();
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
             Q4604      | -0552-10-09T00:00:00Z  | Q736936    |              | 09 October 0553 B.C. | Confucius
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
        NoteRealm note = controller.createNoteUnderParent(parent, noteCreation).getCreated();
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
        noteCreation.setNewTitle("Johnny boy");
        NoteRealm note = controller.createNoteUnderParent(parent, noteCreation).getCreated();

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
        noteCreation.setNewTitle("Harry Potter");
      }

      @Test
      void shouldAddBookNoteWithAuthorNoteWithWikidataId()
          throws BindException,
              InterruptedException,
              UnexpectedNoAccessRightException,
              IOException {
        mockWikidataWBGetEntity(
            "Q8337", makeMe.wikidataClaimsJson("Q8337").asABookWithSingleAuthor("Q34660").please());
        NoteRealm note = controller.createNoteUnderParent(parent, noteCreation).getCreated();

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
        NoteRealm note = controller.createNoteUnderParent(parent, noteCreation).getCreated();

        assertEquals(
            "The girl sat next to the window",
            note.getNote().getChildren().get(1).getTopicConstructor());
      }
    }
  }

  @Nested
  class createNoteAfterTest {
    Note referenceNote;
    NoteCreationDTO noteCreation = new NoteCreationDTO();

    @BeforeEach
    void setup() {
      Note parent = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      referenceNote = makeMe.aNote().under(parent).please();
      makeMe.aNote("next sibling").under(parent).please();
      noteCreation.setNewTitle("new note");
    }

    @Test
    void shouldCreateNoteAfterReferenceNote()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      NoteRealm response = controller.createNoteAfter(referenceNote, noteCreation).getCreated();
      assertThat(response.getId(), not(nullValue()));
      assertThat(response.getNote().getTopicConstructor(), equalTo("new note"));
    }

    @Test
    void shouldNotAllowCreatingSiblingForRootNote() {
      Note rootNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.createNoteAfter(rootNote, noteCreation));
    }

    @Test
    void shouldNotAllowCreatingSiblingForNoteWithoutAccess() {
      User otherUser = makeMe.aUser().please();
      Note otherNote = makeMe.aNote().creatorAndOwner(otherUser).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.createNoteAfter(otherNote, noteCreation));
    }

    @Test
    void shouldInsertNoteAfterReferenceNoteAndBeforeNextSibling()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      NoteRealm response = controller.createNoteAfter(referenceNote, noteCreation).getCreated();
      Note createdNote = makeMe.modelFactoryService.noteRepository.findById(response.getId()).get();

      // Get all siblings in order
      List<Note> siblings = createdNote.getParent().getChildren();

      // Verify order: referenceNote -> createdNote -> "next sibling"
      assertThat(
          siblings.get(0).getTopicConstructor(), equalTo(referenceNote.getTopicConstructor()));
      assertThat(siblings.get(1).getTopicConstructor(), equalTo("new note"));
      assertThat(siblings.get(2).getTopicConstructor(), equalTo("next sibling"));
    }
  }
}
