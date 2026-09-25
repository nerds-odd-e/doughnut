package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

class NotebookFolderPageXmlAcceptMvcTest extends NotebookControllerTestBase {
  @Autowired private MockMvc mockMvc;
  @Autowired private EntityManager entityManager;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private UserRepository userRepository;

  /**
   * Committing the fixture in its own transaction and clearing this test's persistence context
   * afterward forces the folder page request below to load a genuinely detached ancestor chain, the
   * same way a real, non-transactional request does. Building the fixture directly in this test's
   * own {@code @Transactional} session (as other folder-page tests do) would keep every folder in
   * the shared first-level cache, so no lazy proxy would ever appear.
   */
  @Test
  void nestedFolderPageIsServedAsJsonEvenWhenBrowserAcceptHeaderPrefersXml() throws Exception {
    int[] ids = new int[3];
    inCommittedTransaction(
        transactionManager,
        () -> {
          User owner = makeMe.aUser().please();
          Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
          Folder parent = makeMe.aFolder().notebook(nb).name("Parent").please();
          Folder nested = makeMe.aFolder().parentFolder(parent).name("Nested").please();
          ids[0] = owner.getId();
          ids[1] = nb.getId();
          ids[2] = nested.getId();
        });
    entityManager.clear();
    currentUser.setUser(userRepository.findById(ids[0]).orElseThrow());

    mockMvc
        .perform(
            get("/api/notebooks/{notebook}/folders/{folder}", ids[1], ids[2])
                .header(
                    "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"))
        .andExpect(status().isOk());
  }
}
