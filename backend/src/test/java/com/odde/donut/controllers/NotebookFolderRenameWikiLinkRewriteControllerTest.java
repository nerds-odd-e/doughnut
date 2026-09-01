package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.algorithms.Frontmatter;
import com.odde.donut.controllers.dto.FolderRenameRequest;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.ResolvedWikiLinkService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookFolderRenameWikiLinkRewriteControllerTest
    extends NotebookFolderManagementControllerTestBase {

  @Autowired ResolvedWikiLinkService resolvedWikiLinkServiceBean;

  @ParameterizedTest
  @CsvSource({
    "See [[OldFolder/Title]] and [label](/OldFolder/Title.md)., See [[NewFolder/Title]] and [label](/OldFolder/Title.md).",
    "See [[OldFolder/Title#prop:a%20part%20of|shown]]., See [[NewFolder/Title#prop:a%20part%20of|shown]]."
  })
  void folderRename_rewritesInboundReferrerLinks(String before, String after)
      throws UnexpectedNoAccessRightException {
    User owner = currentUser.getUser();
    Notebook nb = ownedNotebook();
    Folder oldFolder = ownedFolder(nb, "OldFolder");
    makeMe
        .aNote("Title")
        .folder(oldFolder)
        .content(Frontmatter.empty().set("a part of", "v").fenced(""))
        .please();
    Note carrier = makeMe.aNote("Carrier").notebook(nb).content(before).please();
    resolvedWikiLinkServiceBean.refreshForNote(carrier, owner);

    FolderRenameRequest req = new FolderRenameRequest();
    req.setName("NewFolder");
    controller.renameFolder(nb, oldFolder, req);

    makeMe.refresh(carrier);
    assertThat(carrier.getContent(), equalTo(after));
  }
}
