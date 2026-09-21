package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class NotebookAccessDenialMvcTest extends NotebookGitBundleControllerTestBase {
  @Autowired private MockMvc mockMvc;

  @ParameterizedTest
  @ValueSource(strings = {"", "/git-bundle"})
  void deniedReadReturnsEmptyForbiddenResponse(String suffix) throws Exception {
    Notebook notebook = createGitBackedNotebook();
    currentUser.setUser(createFixtureUser());

    mockMvc
        .perform(get("/api/notebooks/" + notebook.getId() + suffix))
        .andExpect(status().isForbidden())
        .andExpect(content().string(""));
  }

  @Test
  void deniedPublicationReturnsEmptyForbiddenResponseWithoutChangingAcceptedData()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("Private note").content("Private content").please();
    snapshotCurrentPortableTree(notebook);
    NotebookGitBinding before =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    String acceptedHead = before.getAcceptedGitObjectId();
    byte[] acceptedBundle = acceptedBundleBytes(notebook);
    var acceptedHistoryBefore = acceptedHistory(notebook);
    var updatedAt = before.getUpdatedAt().toInstant();
    User owner = currentUser.getUser();
    currentUser.setUser(createFixtureUser());

    mockMvc
        .perform(
            post("/api/notebooks/{notebook}/git-bundle", notebook.getId())
                .param("expectedHead", acceptedHead)
                .contentType("application/x-git-bundle")
                .content(acceptedBundle))
        .andExpect(status().isForbidden())
        .andExpect(content().string(""));

    currentUser.setUser(owner);
    NotebookGitBinding after =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    assertThat(after.getAcceptedGitObjectId(), equalTo(acceptedHead));
    assertThat(acceptedHistory(notebook), equalTo(acceptedHistoryBefore));
    assertThat(after.getUpdatedAt().toInstant(), equalTo(updatedAt));
    Note unchanged = noteRepository.findById(note.getId()).orElseThrow();
    assertThat(unchanged.getTitle(), equalTo("Private note"));
    assertThat(unchanged.getContent(), equalTo("Private content"));
  }
}
