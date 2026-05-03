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

class NotebookRootNoteCreationWithWikidataTests extends ControllerTestBase {
  @Autowired NoteRepository noteRepository;
  @Autowired MakeMe makeMe;
  @Autowired NotebookController notebookController;
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
  class createNoteInFolderTest {
    Notebook notebook;
    NoteCreationDTO noteCreation = new NoteCreationDTO();

    @BeforeEach
    void setup() {
      notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      noteCreation.setNewTitle("new title");
    }

    @Test
    void assignsNoteToExplicitFolderById()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      Folder folder = makeMe.aFolder().notebook(notebook).name("ExplicitParent").please();
      noteCreation.setFolderId(folder.getId());
      NoteRealm response = notebookController.createNoteAtNotebookRoot(notebook, noteCreation);
      Note created = noteRepository.findById(response.getId()).orElseThrow();
      assertThat(created.getFolder(), not(nullValue()));
      assertThat(created.getFolder().getName(), equalTo("ExplicitParent"));
      assertThat(created.getFolder().getNotebook().getId(), equalTo(notebook.getId()));
    }

    @Test
    void assignsNestedFolderHierarchyForLeaf()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      Folder bookHead = makeMe.aFolder().notebook(notebook).name("BookHead").please();
      Folder section =
          makeMe.aFolder().notebook(notebook).name("Section").parentFolder(bookHead).please();
      noteCreation.setNewTitle("Leaf");
      noteCreation.setFolderId(section.getId());
      Note leaf =
          noteRepository
              .findById(notebookController.createNoteAtNotebookRoot(notebook, noteCreation).getId())
              .orElseThrow();
      assertThat(leaf.getFolder(), not(nullValue()));
      assertThat(leaf.getFolder().getName(), equalTo("Section"));
      assertThat(leaf.getFolder().getParentFolder(), not(nullValue()));
      assertThat(leaf.getFolder().getParentFolder().getName(), equalTo("BookHead"));
    }

    @Test
    void childNoteUsesNamedFolder()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      Folder folder = makeMe.aFolder().notebook(notebook).name("ExplicitParent").please();
      noteCreation.setFolderId(folder.getId());
      NoteRealm response = notebookController.createNoteAtNotebookRoot(notebook, noteCreation);
      Note created = noteRepository.findById(response.getId()).orElseThrow();
      assertThat(created.getFolder().getName(), equalTo("ExplicitParent"));
    }

    @Test
    void nestedFolderNamesMatchAncestorChain()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      Folder bookHead = makeMe.aFolder().notebook(notebook).name("BookHead").please();
      Folder section =
          makeMe.aFolder().notebook(notebook).name("Section").parentFolder(bookHead).please();
      noteCreation.setNewTitle("Leaf");
      noteCreation.setFolderId(section.getId());
      Note leaf =
          noteRepository
              .findById(notebookController.createNoteAtNotebookRoot(notebook, noteCreation).getId())
              .orElseThrow();
      assertThat(section.getName(), equalTo("Section"));
      assertThat(leaf.getFolder().getName(), equalTo("Section"));
      assertThat(leaf.getFolder().getParentFolder().getName(), equalTo("BookHead"));
    }

    @Test
    void duplicateTitlesUnderSameFolder_shareFolder()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      Folder folder = makeMe.aFolder().notebook(notebook).name("SharedParent").please();
      noteCreation.setFolderId(folder.getId());
      noteCreation.setNewTitle("dup");
      Note n1 =
          noteRepository
              .findById(notebookController.createNoteAtNotebookRoot(notebook, noteCreation).getId())
              .orElseThrow();
      noteCreation.setNewTitle("dup");
      Note n2 =
          noteRepository
              .findById(notebookController.createNoteAtNotebookRoot(notebook, noteCreation).getId())
              .orElseThrow();
      assertThat(n1.getFolder().getId(), equalTo(n2.getFolder().getId()));
      assertThat(n1.getId(), not(equalTo(n2.getId())));
    }

    @Test
    void siblingsShareSameFolder()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      Folder folder = makeMe.aFolder().notebook(notebook).name("SharedParent").please();
      noteCreation.setFolderId(folder.getId());
      noteCreation.setNewTitle("first child");
      Integer id1 = notebookController.createNoteAtNotebookRoot(notebook, noteCreation).getId();
      noteCreation.setNewTitle("second child");
      Integer id2 = notebookController.createNoteAtNotebookRoot(notebook, noteCreation).getId();
      Note n1 = noteRepository.findById(id1).orElseThrow();
      Note n2 = noteRepository.findById(id2).orElseThrow();
      assertThat(n1.getFolder().getId(), equalTo(n2.getFolder().getId()));
    }

    @Test
    void shouldBeAbleToSaveNoteWhenValid()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      Folder folder = makeMe.aFolder().notebook(notebook).name("Placement").please();
      noteCreation.setFolderId(folder.getId());
      NoteRealm response = notebookController.createNoteAtNotebookRoot(notebook, noteCreation);
      assertThat(response.getId(), not(nullValue()));
      Note created = noteRepository.findById(response.getId()).orElseThrow();
      assertThat(created.getFolder(), not(nullValue()));
      assertThat(created.getFolder().getNotebook().getId(), equalTo(notebook.getId()));
    }

    @Test
    void shouldBeAbleToCreateAThing()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      Folder folder = makeMe.aFolder().notebook(notebook).name("Rootish").please();
      noteCreation.setFolderId(folder.getId());
      long beforeThingCount = noteRepository.count();
      notebookController.createNoteAtNotebookRoot(notebook, noteCreation);
      long afterThingCount = noteRepository.count();
      assertThat(afterThingCount, equalTo(beforeThingCount + 1));
    }

    @Test
    void shouldBeAbleToSaveNoteWithWikidataIdWhenValid()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      Folder folder = makeMe.aFolder().notebook(notebook).name("WikidataHome").please();
      noteCreation.setFolderId(folder.getId());
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(new MakeMeWithoutDB().wikidataEntityJson().entityId("Q12345").please());
      noteCreation.setWikidataId("Q12345");
      NoteRealm response = notebookController.createNoteAtNotebookRoot(notebook, noteCreation);
      assertThat(response.getNote().getWikidataId(), equalTo("Q12345"));
    }

    @Test
    void shouldBeAbleToSaveNoteWithoutWikidataIdWhenValid()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      Folder folder = makeMe.aFolder().notebook(notebook).name("PlainHome").please();
      noteCreation.setFolderId(folder.getId());
      NoteRealm response = notebookController.createNoteAtNotebookRoot(notebook, noteCreation);
      assertThat(response.getNote().getWikidataId(), equalTo(null));
    }

    @Test
    void shouldThrowWhenCreatingNoteWithWikidataIdExistsInAnotherNote() {
      Folder folder = makeMe.aFolder().notebook(notebook).name("DupW").please();
      String conflictingWikidataId = "Q123";
      makeMe
          .aNote()
          .creatorAndOwner(currentUser.getUser())
          .inNotebook(notebook)
          .folder(folder)
          .wikidataId(conflictingWikidataId)
          .please();
      noteCreation.setFolderId(folder.getId());
      noteCreation.setWikidataId(conflictingWikidataId);
      BindException bindException =
          assertThrows(
              BindException.class,
              () -> notebookController.createNoteAtNotebookRoot(notebook, noteCreation));
      assertThat(
          bindException.getMessage(), stringContainsInOrder("Duplicate Wikidata ID Detected."));
    }

    @Nested
    class AddingNoteWithLocationWikidataId {
      String wikidataIdOfALocation = "Q334";
      String lnglat = "1.3'N, 103.8'E";
      Folder folder;

      @BeforeEach
      void thereIsAWikidataEntryOfALocation() {
        folder = makeMe.aFolder().notebook(notebook).name("Places").please();
        noteCreation.setFolderId(folder.getId());
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
        NoteRealm note = notebookController.createNoteAtNotebookRoot(notebook, noteCreation);
        assertThat(note.getNote().getDetails(), containsString("Location: " + lnglat));
      }

      @Test
      void shouldPrependLocationInfoWhenAddingNoteWithWikidataIdWithStringValue()
          throws BindException,
              InterruptedException,
              UnexpectedNoAccessRightException,
              IOException {
        mockApiResponseWithLocationInfo("\"center of the earth\"", "string");
        NoteRealm note = notebookController.createNoteAtNotebookRoot(notebook, noteCreation);
        assertThat(
            note.getNote().getDetails(), stringContainsInOrder("Location: center of the earth"));
      }
    }

    @Nested
    class AddingNoteWithHumanWikidataId {
      Folder folder;

      @BeforeEach
      void thereIsAWikidataEntryOfAHuman() {
        folder = makeMe.aFolder().notebook(notebook).name("People").please();
        noteCreation.setFolderId(folder.getId());
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
        NoteRealm note = notebookController.createNoteAtNotebookRoot(notebook, noteCreation);
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
        NoteRealm note = notebookController.createNoteAtNotebookRoot(notebook, noteCreation);

        assertEquals("Johnny boy", note.getNote().getTitle());
        assertEquals("Q8337", note.getNote().getWikidataId());
        Note persisted = noteRepository.findById(note.getId()).orElseThrow();
        assertThat(persisted.getFolder(), not(nullValue()));
        List<String> peerTitles =
            noteRepository.findNotesInFolderOrderByIdAsc(persisted.getFolder().getId()).stream()
                .map(Note::getTitle)
                .toList();
        assertThat(peerTitles, hasItems("Johnny boy", "Canada"));
      }
    }

    @Nested
    class AddingBookNoteWithAuthorInformation {
      Folder folder;

      @BeforeEach
      void setup() throws IOException, InterruptedException {
        folder = makeMe.aFolder().notebook(notebook).name("Books").please();
        noteCreation.setFolderId(folder.getId());
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
        NoteRealm note = notebookController.createNoteAtNotebookRoot(notebook, noteCreation);

        assertEquals("Harry Potter", note.getNote().getTitle());
        assertEquals("Q8337", note.getNote().getWikidataId());
        Note persisted = noteRepository.findById(note.getId()).orElseThrow();
        assertThat(persisted.getFolder(), not(nullValue()));
        List<String> peerTitles =
            noteRepository.findNotesInFolderOrderByIdAsc(persisted.getFolder().getId()).stream()
                .map(Note::getTitle)
                .toList();
        assertThat(peerTitles, hasItems("Harry Potter", "J. K. Rowling"));
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
        NoteRealm note = notebookController.createNoteAtNotebookRoot(notebook, noteCreation);

        assertEquals("Harry Potter", note.getNote().getTitle());
        assertEquals("Q8337", note.getNote().getWikidataId());
        Note persisted = noteRepository.findById(note.getId()).orElseThrow();
        assertThat(persisted.getFolder(), not(nullValue()));
        List<String> peerTitles =
            noteRepository.findNotesInFolderOrderByIdAsc(persisted.getFolder().getId()).stream()
                .map(Note::getTitle)
                .toList();
        assertThat(
            peerTitles,
            hasItems("Harry Potter", "J. K. Rowling", "The girl sat next to the window"));
      }
    }
  }
}
