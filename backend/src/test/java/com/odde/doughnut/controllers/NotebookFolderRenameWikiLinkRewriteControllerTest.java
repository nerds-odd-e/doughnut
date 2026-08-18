package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.controllers.dto.FolderRenameRequest;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.WikiTitleCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookFolderRenameWikiLinkRewriteControllerTest
    extends NotebookFolderManagementControllerTestBase {

  @Autowired WikiTitleCacheService wikiTitleCacheServiceBean;

  @Test
  void folderRename_rewritesPathPrefixInWikiAndMarkdownSpellings()
      throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook nb = ownedNotebook();
    Folder oldFolder = ownedFolder(nb, "OldFolder");
    makeMe.aNote("Title").folder(oldFolder).please();
    Note carrier =
        makeMe
            .aNote("Carrier")
            .notebook(nb)
            .content("See [[OldFolder/Title]] and [label](/OldFolder/Title.md).")
            .please();
    wikiTitleCacheServiceBean.refreshForNote(carrier, owner);

    FolderRenameRequest req = new FolderRenameRequest();
    req.setName("NewFolder");
    controller.renameFolder(nb, oldFolder, req);

    makeMe.refresh(carrier);
    assertThat(
        carrier.getContent(), equalTo("See [[NewFolder/Title]] and [label](/NewFolder/Title.md)."));
  }
}
