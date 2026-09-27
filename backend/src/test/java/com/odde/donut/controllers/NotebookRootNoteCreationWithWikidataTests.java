package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import com.odde.donut.controllers.dto.*;
import com.odde.donut.entities.*;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.GitBundleTestReader.SingleParentGitCommit;
import com.odde.donut.testability.MakeMeWithoutDB;
import java.io.IOException;
import java.net.URI;
import org.apache.logging.log4j.util.Strings;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.validation.BindException;

class NotebookRootNoteCreationWithWikidataTests extends NotebookControllerTestBase {
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
  class CreateNoteWithWikidata {
    Notebook notebook;
    Folder defaultFolder;
    NoteCreationDTO noteCreation = new NoteCreationDTO();

    @BeforeEach
    void setup() {
      notebook = ownedNotebook();
      defaultFolder = makeMe.aFolder().notebook(notebook).name("Default").please();
      noteCreation.setNewTitle("new title");
      noteCreation.setContent(null);
      noteCreation.setFolderId(defaultFolder.getId());
    }

    @Test
    void shouldBeAbleToSaveNoteWithWikidataIdWhenValid()
        throws UnexpectedNoAccessRightException, BindException, InterruptedException, IOException {
      Mockito.when(httpClientAdapter.getResponseString(any()))
          .thenReturn(new MakeMeWithoutDB().wikidataEntityJson().entityId("Q12345").please());
      noteCreation.setContent("---\nwikidata_id: Q12345\n---\n");
      NoteRealm response = controller.createNoteAtNotebookRoot(notebook, noteCreation);
      assertThat(response.getNote().getContent(), containsString("wikidata_id: Q12345"));
    }

    @Test
    void wikidataAssistedRootCreateIsAcceptedInOneCommit() throws Exception {
      NotebookCreationRequest request = new NotebookCreationRequest();
      request.setNewTitle("Wikidata Git Notebook");
      Notebook gitNotebook =
          notebookRepository
              .findById(controller.createNotebook(request).notebook().getId())
              .orElseThrow();
      ObjectId acceptedHead =
          ObjectId.fromString(
              notebookGitBindingRepository
                  .findByNotebook_Id(gitNotebook.getId())
                  .orElseThrow()
                  .getAcceptedGitObjectId());
      mockWikidataWBGetEntity(
          "Q334",
          makeMe
              .wikidataClaimsJson("Q334")
              .globeCoordinate("{\"latitude\":1.3,\"longitude\":103.8}", "globecoordinate")
              .please());
      NoteCreationDTO creation = new NoteCreationDTO();
      creation.setNewTitle("Wikidata Root");
      creation.setContent("---\nwikidata_id: Q334\n---\n");

      controller.createNoteAtNotebookRoot(gitNotebook, creation);

      byte[] downloaded = controller.downloadNotebookGitBundle(gitNotebook).getBody();
      try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
        SingleParentGitCommit accepted =
            GitBundleTestReader.fetchSingleParentCommit(repository, downloaded);
        assertThat(accepted.parent(), is(acceptedHead));
        assertThat(
            NotebookGitProposalBlobText.readUtf8(repository, accepted.head(), "Wikidata Root.md"),
            containsString("Location: 1.3'N, 103.8'E"));
      }
    }

    @Nested
    class AddingNoteWithLocationWikidataId {
      String wikidataIdOfALocation = "Q334";
      String lnglat = "1.3'N, 103.8'E";

      @BeforeEach
      void thereIsAWikidataEntryOfALocation() {
        Folder folder = makeMe.aFolder().notebook(notebook).name("Places").please();
        noteCreation.setFolderId(folder.getId());
        noteCreation.setContent("---\nwikidata_id: " + wikidataIdOfALocation + "\n---\n");
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
        NoteRealm note = controller.createNoteAtNotebookRoot(notebook, noteCreation);
        assertThat(note.getNote().getContent(), startsWith("---\ntype: Note\n"));
        assertThat(note.getNote().getContent(), containsString("Location: " + lnglat));
      }

      @Test
      void shouldPrependLocationInfoWhenAddingNoteWithWikidataIdWithStringValue()
          throws BindException,
              InterruptedException,
              UnexpectedNoAccessRightException,
              IOException {
        mockApiResponseWithLocationInfo("\"center of the earth\"", "string");
        NoteRealm note = controller.createNoteAtNotebookRoot(notebook, noteCreation);
        assertThat(
            note.getNote().getContent(), stringContainsInOrder("Location: center of the earth"));
      }
    }

    @Nested
    class AddingNoteWithHumanWikidataId {
      @BeforeEach
      void thereIsAWikidataEntryOfAHuman() {
        Folder folder = makeMe.aFolder().notebook(notebook).name("People").please();
        noteCreation.setFolderId(folder.getId());
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
        noteCreation.setContent("---\nwikidata_id: " + wikidataIdOfHuman + "\n---\n");
        NoteRealm note = controller.createNoteAtNotebookRoot(notebook, noteCreation);
        String description = note.getNote().getContent();
        if (expectedBirthday != null) {
          assertThat(description, containsString(expectedBirthday));
        }
        if (countryName != null) {
          assertThat(description, containsString(countryName));
        }
      }
    }
  }
}
