package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.MakeMeWithoutDB;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindException;

class NoteCreationControllerTests extends ControllerTestBase {
  @Autowired NoteRepository noteRepository;
  @Autowired MakeMe makeMe;
  @Autowired NoteCreationController controller;
  @MockitoBean HttpClientAdapter httpClientAdapter;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
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
    void assignsFolderNamedAfterParentTitle()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      parent =
          makeMe.aNote().title("ExplicitParent").creatorAndOwner(currentUser.getUser()).please();
      NoteRealm response = controller.createNoteUnderParent(parent, noteCreation);
      Note created = noteRepository.findById(response.getId()).orElseThrow();
      assertThat(created.getFolder(), not(nullValue()));
      assertThat(created.getFolder().getName(), equalTo("ExplicitParent"));
      assertThat(created.getFolder().getNotebook().getId(), equalTo(parent.getNotebook().getId()));
    }

    @Test
    void assignsNestedChildContainerWithParentFolder()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      Note bookRoot =
          makeMe.aNote().title("BookHead").creatorAndOwner(currentUser.getUser()).please();
      NoteCreationDTO sectionDto = new NoteCreationDTO();
      sectionDto.setNewTitle("Section");
      Note section =
          noteRepository
              .findById(controller.createNoteUnderParent(bookRoot, sectionDto).getId())
              .orElseThrow();
      noteCreation.setNewTitle("Leaf");
      Note leaf =
          noteRepository
              .findById(controller.createNoteUnderParent(section, noteCreation).getId())
              .orElseThrow();
      assertThat(leaf.getFolder(), not(nullValue()));
      assertThat(leaf.getFolder().getName(), equalTo("Section"));
      assertThat(leaf.getFolder().getParentFolder(), not(nullValue()));
      assertThat(leaf.getFolder().getParentFolder().getName(), equalTo("BookHead"));
    }

    @Test
    void assignsSlugsToFolderAndNoteFromTitles()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      parent =
          makeMe.aNote().title("ExplicitParent").creatorAndOwner(currentUser.getUser()).please();
      NoteRealm response = controller.createNoteUnderParent(parent, noteCreation);
      Note created = noteRepository.findById(response.getId()).orElseThrow();
      assertThat(created.getFolder().getSlug(), equalTo("explicitparent"));
      assertThat(created.getSlug(), equalTo("explicitparent/new-title"));
    }

    @Test
    void assignsNestedSlugsForFolderPathAndNote()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      Note bookRoot =
          makeMe.aNote().title("BookHead").creatorAndOwner(currentUser.getUser()).please();
      NoteCreationDTO sectionDto = new NoteCreationDTO();
      sectionDto.setNewTitle("Section");
      Note section =
          noteRepository
              .findById(controller.createNoteUnderParent(bookRoot, sectionDto).getId())
              .orElseThrow();
      noteCreation.setNewTitle("Leaf");
      Note leaf =
          noteRepository
              .findById(controller.createNoteUnderParent(section, noteCreation).getId())
              .orElseThrow();
      assertThat(section.getFolder().getSlug(), equalTo("bookhead"));
      assertThat(leaf.getFolder().getSlug(), equalTo("bookhead/section"));
      assertThat(leaf.getSlug(), equalTo("bookhead/section/leaf"));
    }

    @Test
    void assignsUniqueNoteSlugWhenSameTitleUnderSameFolder()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      parent = makeMe.aNote().title("SharedParent").creatorAndOwner(currentUser.getUser()).please();
      noteCreation.setNewTitle("dup");
      Note n1 =
          noteRepository
              .findById(controller.createNoteUnderParent(parent, noteCreation).getId())
              .orElseThrow();
      assertThat(n1.getSlug(), equalTo("sharedparent/dup"));
      noteCreation.setNewTitle("dup");
      Note n2 =
          noteRepository
              .findById(controller.createNoteUnderParent(parent, noteCreation).getId())
              .orElseThrow();
      assertThat(n2.getSlug(), equalTo("sharedparent/dup-2"));
    }

    @Test
    void siblingsShareSameChildContainerFolder()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      parent = makeMe.aNote().title("SharedParent").creatorAndOwner(currentUser.getUser()).please();
      noteCreation.setNewTitle("first child");
      Integer id1 = controller.createNoteUnderParent(parent, noteCreation).getId();
      noteCreation.setNewTitle("second child");
      Integer id2 = controller.createNoteUnderParent(parent, noteCreation).getId();
      Note n1 = noteRepository.findById(id1).orElseThrow();
      Note n2 = noteRepository.findById(id2).orElseThrow();
      assertThat(n1.getFolder().getId(), equalTo(n2.getFolder().getId()));
    }

    @Test
    void shouldBeAbleToSaveNoteWhenValid()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      NoteRealm response = controller.createNoteUnderParent(parent, noteCreation);
      assertThat(response.getId(), not(nullValue()));
      assertThat(
          noteRepository.findById(response.getId()).orElseThrow().getParent().getId(),
          equalTo(parent.getId()));
    }

    @Test
    void shouldBeAbleToCreateAThing()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      long beforeThingCount = noteRepository.count();
      controller.createNoteUnderParent(parent, noteCreation);
      long afterThingCount = noteRepository.count();
      assertThat(afterThingCount, equalTo(beforeThingCount + 1));
    }

    @Test
    void shouldBeAbleToSaveNoteWithWikidataIdWhenValid()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(new MakeMeWithoutDB().wikidataEntityJson().entityId("Q12345").please());
      noteCreation.setWikidataId("Q12345");
      NoteRealm response = controller.createNoteUnderParent(parent, noteCreation);
      assertThat(response.getNote().getWikidataId(), equalTo("Q12345"));
    }

    @Test
    void shouldBeAbleToSaveNoteWithoutWikidataIdWhenValid()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      NoteRealm response = controller.createNoteUnderParent(parent, noteCreation);
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
        NoteRealm note = controller.createNoteUnderParent(parent, noteCreation);
        assertThat(note.getNote().getDetails(), containsString("Location: " + lnglat));
      }

      @Test
      void shouldPrependLocationInfoWhenAddingNoteWithWikidataIdWithStringValue()
          throws BindException,
              InterruptedException,
              UnexpectedNoAccessRightException,
              IOException {
        mockApiResponseWithLocationInfo("\"center of the earth\"", "string");
        NoteRealm note = controller.createNoteUnderParent(parent, noteCreation);
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
        NoteRealm note = controller.createNoteUnderParent(parent, noteCreation);
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
        NoteRealm note = controller.createNoteUnderParent(parent, noteCreation);

        assertEquals("Johnny boy", note.getNote().getTitle());
        assertEquals("Q8337", note.getNote().getWikidataId());
        List<String> siblingTitlesUnderParent =
            noteRepository.findById(parent.getId()).orElseThrow().getChildren().stream()
                .map(Note::getTitle)
                .toList();
        assertThat(siblingTitlesUnderParent, hasItems("Johnny boy", "Canada"));
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
        NoteRealm note = controller.createNoteUnderParent(parent, noteCreation);

        assertEquals("Harry Potter", note.getNote().getTitle());
        assertEquals("Q8337", note.getNote().getWikidataId());
        List<String> siblingTitlesUnderParent =
            noteRepository.findById(parent.getId()).orElseThrow().getChildren().stream()
                .map(Note::getTitle)
                .toList();
        assertThat(siblingTitlesUnderParent, hasItems("Harry Potter", "J. K. Rowling"));
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
        NoteRealm note = controller.createNoteUnderParent(parent, noteCreation);

        assertEquals("Harry Potter", note.getNote().getTitle());
        assertEquals("Q8337", note.getNote().getWikidataId());
        List<String> siblingTitlesUnderParent =
            noteRepository.findById(parent.getId()).orElseThrow().getChildren().stream()
                .map(Note::getTitle)
                .toList();
        assertThat(
            siblingTitlesUnderParent,
            hasItems("Harry Potter", "J. K. Rowling", "The girl sat next to the window"));
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
      NoteRealm response = controller.createNoteAfter(referenceNote, noteCreation);
      assertThat(response.getId(), not(nullValue()));
      assertThat(response.getNote().getTitle(), equalTo("new note"));
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
      NoteRealm response = controller.createNoteAfter(referenceNote, noteCreation);
      Note createdNote = noteRepository.findById(response.getId()).get();

      // Get all siblings in order
      List<Note> siblings = createdNote.getParent().getChildren();

      // Verify order: referenceNote -> createdNote -> "next sibling"
      assertThat(siblings.get(0).getTitle(), equalTo(referenceNote.getTitle()));
      assertThat(siblings.get(1).getTitle(), equalTo("new note"));
      assertThat(siblings.get(2).getTitle(), equalTo("next sibling"));
    }
  }
}
