package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WikiSlugPathServiceSlugFallbackTest {

  @Autowired MakeMe makeMe;
  @Autowired WikiSlugPathService wikiSlugPathService;
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void assignSlug_usesNotebookScopedSlugWhenFolderPathLeavesNoBasenameRoom() {
    User owner = makeMe.aUser().please();
    Note root = makeMe.aNote().title("Root").creatorAndOwner(owner).please();
    Notebook notebook = root.getNotebook();
    int rootId = root.getId();
    Folder folder = makeMe.aFolder().notebook(notebook).please();
    int folderId = folder.getId();
    jdbcTemplate.update("UPDATE folder SET slug = ? WHERE id = ?", "y".repeat(766), folderId);
    makeMe.entityPersister.flush();
    makeMe.entityPersister.flushAndClear();
    Folder folderReloaded = makeMe.entityPersister.find(Folder.class, folderId);
    Note rootReloaded = makeMe.entityPersister.find(Note.class, rootId);

    Note leaf =
        makeMe
            .aNote()
            .title("Leaf")
            .creatorAndOwner(owner)
            .folder(folderReloaded)
            .under(rootReloaded)
            .please();

    assertThat(leaf.getSlug().length(), lessThanOrEqualTo(Note.MAX_SLUG_LENGTH));
    assertThat(leaf.getSlug().contains("/"), equalTo(false));

    wikiSlugPathService.assignSlugForNewNote(leaf);
    assertThat(leaf.getSlug().startsWith("nid"), equalTo(true));
    assertThat(leaf.getSlug().length(), lessThanOrEqualTo(Note.MAX_SLUG_LENGTH));
  }
}
