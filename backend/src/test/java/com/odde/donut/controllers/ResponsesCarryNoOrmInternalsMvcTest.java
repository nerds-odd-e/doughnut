package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The fixture is committed and this test's persistence context cleared, so the request loads the
 * nested folders as genuine lazy proxies, the way a real, non-transactional request does. Building
 * it in this test's own transaction would keep every folder in the first-level cache.
 */
class ResponsesCarryNoOrmInternalsMvcTest extends ControllerTestBase {
  @Autowired private MockMvc mockMvc;
  @Autowired private EntityManager entityManager;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private UserRepository userRepository;

  private int notebookId;
  private int innerFolderId;
  private int attachmentId;
  private int noteId;

  @BeforeEach
  void setup() {
    int[] ownerId = new int[1];
    inCommittedTransaction(
        transactionManager,
        () -> {
          User owner = makeMe.aUser().please();
          Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
          Folder outer = makeMe.aFolder().notebook(nb).name("outer").please();
          Folder inner = makeMe.aFolder().parentFolder(outer).name("inner").please();
          ownerId[0] = owner.getId();
          notebookId = nb.getId();
          innerFolderId = inner.getId();
          attachmentId = makeMe.anAttachment("a.txt").in(inner).please().getId();
          noteId = makeMe.aNote().folder(inner).please().getId();
        });
    entityManager.clear();
    currentUser.setUser(userRepository.findById(ownerId[0]).orElseThrow());
  }

  @Test
  void filePageCarriesTheTrailWithoutProxyInternals() throws Exception {
    assertCleanTrail(
        mockMvc.perform(
            get("/api/notebooks/{notebook}/attachments/{attachment}", notebookId, attachmentId)
                .accept(MediaType.APPLICATION_JSON)));
  }

  @Test
  void noteRealmCarriesTheTrailWithoutProxyInternals() throws Exception {
    assertCleanTrail(
        mockMvc.perform(get("/api/notes/{note}", noteId).accept(MediaType.APPLICATION_JSON)));
  }

  @Test
  void nestedFolderPageIsServedAsJsonEvenWhenBrowserAcceptHeaderPrefersXml() throws Exception {
    mockMvc
        .perform(
            get("/api/notebooks/{notebook}/folders/{folder}", notebookId, innerFolderId)
                .header(
                    "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"))
        .andExpect(status().isOk());
  }

  private void assertCleanTrail(ResultActions result) throws Exception {
    result
        .andExpect(status().isOk())
        .andExpect(jsonPath("$..ancestorFolders[*].name", contains("outer", "inner")))
        .andExpect(content().string(not(Matchers.containsString("hibernateLazyInitializer"))));
  }
}
