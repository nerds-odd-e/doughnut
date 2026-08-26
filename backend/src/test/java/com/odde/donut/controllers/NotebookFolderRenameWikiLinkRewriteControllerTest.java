package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.controllers.dto.FolderRenameRequest;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.WikiTitleCacheService;
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
