package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteWikiTitleCache;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NoteRealmServiceTest {

  @Autowired com.odde.doughnut.testability.MakeMe makeMe;
  @Autowired NoteRealmService noteRealmService;
  @Autowired NoteWikiTitleCacheRepository noteWikiTitleCacheRepository;
  @Autowired WikiTitleCacheService wikiTitleCacheService;

  @Test
  void wiki_titles_empty_when_content_has_links_but_cache_not_refreshed() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    makeMe.aNote().title("LinkedPage").underSameNotebookAs(root).please();
    Note carrier = makeMe.aNote().underSameNotebookAs(root).content("[[LinkedPage]]").please();

    NoteRealm realm = noteRealmService.build(carrier, user);

    assertThat(realm.getWikiTitles(), empty());
  }

  @Test
  void omits_cached_target_when_viewer_cannot_read_target_notebook() {
    User otherUser = makeMe.aUser().please();
    Note headSecret = makeMe.aNote().creatorAndOwner(otherUser).title("SecretNb").please();
    Note hidden = makeMe.aNote().title("Hidden").underSameNotebookAs(headSecret).please();

    User viewer = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(viewer).please();
    Note carrier = makeMe.aNote().underSameNotebookAs(root).content("plain").please();

    NoteWikiTitleCache row = new NoteWikiTitleCache();
    row.setNote(carrier);
    row.setTargetNote(hidden);
    row.setLinkText("SecretNb:Hidden");
    noteWikiTitleCacheRepository.save(row);

    NoteRealm realm = noteRealmService.build(carrier, viewer);

    assertThat(realm.getWikiTitles(), empty());
  }

  @Test
  void omits_cached_target_when_target_note_is_soft_deleted() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Note target = makeMe.aNote().title("Target").creator(user).underSameNotebookAs(root).please();
    Note carrier = makeMe.aNote().underSameNotebookAs(root).content("[[Target]]").please();
    wikiTitleCacheService.refreshForNote(carrier, user);

    target.setDeletedAt(new Timestamp(System.currentTimeMillis()));
    makeMe.entityPersister.merge(target);

    NoteRealm realm = noteRealmService.build(carrier, user);

    assertThat(realm.getWikiTitles(), empty());
  }

  @Test
  void references_empty_when_cache_rows_deleted_for_relation_carrier() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Note focal = makeMe.aNote().title("Focal").underSameNotebookAs(root).please();
    Note subject = makeMe.aNote().underSameNotebookAs(root).please();
    Note relation = makeMe.aNote().withWikiLinksInFrontmatter(subject, focal).please();
    noteWikiTitleCacheRepository.deleteByNote_Id(relation.getId());

    NoteRealm realm = noteRealmService.build(focal, user);

    assertThat(realm.getReferences(), empty());
  }

  @Test
  void body_wikilink_carrier_in_references() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Note focal = makeMe.aNote().title("Focal").creator(user).underSameNotebookAs(root).please();
    Note carrier =
        makeMe.aNote().creator(user).underSameNotebookAs(root).content("[[Focal]]").please();
    wikiTitleCacheService.refreshForNote(carrier, user);

    NoteRealm realm = noteRealmService.build(focal, user);

    assertThat(realm.getReferences(), hasSize(1));
    assertThat(realm.getReferences().get(0).getId(), equalTo(carrier.getId()));
  }

  @Test
  void parent_yaml_carrier_appears_in_references() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Note focal = makeMe.aNote().title("Focal").creator(user).underSameNotebookAs(root).please();
    Note carrier = makeMe.aNote().title("Child").creator(user).underSameNotebookAs(root).please();
    carrier.setContent("---\nparent: \"[[Focal]]\"\n---\n\nBody.");
    makeMe.entityPersister.merge(carrier);
    makeMe.entityPersister.flush();
    wikiTitleCacheService.refreshForNote(carrier, user);

    NoteRealm realm = noteRealmService.build(focal, user);

    assertThat(realm.getReferences(), hasSize(1));
    assertThat(realm.getReferences().get(0).getId(), equalTo(carrier.getId()));
  }

  @Test
  void references_omit_soft_deleted_relation_even_if_cache_row_remains() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Note focal = makeMe.aNote().title("Focal").underSameNotebookAs(root).please();
    Note subject = makeMe.aNote().underSameNotebookAs(root).please();
    Note relation = makeMe.aNote().withWikiLinksInFrontmatter(subject, focal).please();
    relation.setContent(
        RelationshipNoteMarkdownFormatter.formatForRelationshipNote(
            relation, "a specialization of", subject, focal, null));
    makeMe.entityPersister.merge(relation);
    makeMe.entityPersister.flush();
    wikiTitleCacheService.refreshForNote(relation, user);

    relation.setDeletedAt(new Timestamp(System.currentTimeMillis()));
    makeMe.entityPersister.merge(relation);

    NoteRealm realm = noteRealmService.build(subject, user);

    assertThat(realm.getReferences(), empty());
  }

  @Test
  void references_include_cross_notebook_carrier_when_viewer_can_refer() {
    User user = makeMe.aUser().please();
    Note headMain = makeMe.aNote().creatorAndOwner(user).title("MainNb").please();
    Note focal = makeMe.aNote().title("Focal").creator(user).underSameNotebookAs(headMain).please();
    Note headOther = makeMe.aNote().creatorAndOwner(user).title("OtherNb").please();
    Note carrier = makeMe.aNote().creator(user).underSameNotebookAs(headOther).please();

    NoteWikiTitleCache row = new NoteWikiTitleCache();
    row.setNote(carrier);
    row.setTargetNote(focal);
    row.setLinkText("MainNb:Focal");
    noteWikiTitleCacheRepository.save(row);

    NoteRealm realm = noteRealmService.build(focal, user);

    assertThat(realm.getReferences(), hasSize(1));
    assertThat(realm.getReferences().get(0).getId(), equalTo(carrier.getId()));
  }

  @Test
  void references_omit_cross_notebook_carrier_when_viewer_cannot_refer() {
    User ownerFocal = makeMe.aUser().please();
    User ownerCarrier = makeMe.aUser().please();
    Note headMain = makeMe.aNote().creatorAndOwner(ownerFocal).title("MainNb").please();
    Note focal =
        makeMe.aNote().title("Focal").creator(ownerFocal).underSameNotebookAs(headMain).please();
    Note headOther = makeMe.aNote().creatorAndOwner(ownerCarrier).title("OtherNb").please();
    Note carrier = makeMe.aNote().creator(ownerCarrier).underSameNotebookAs(headOther).please();

    NoteWikiTitleCache row = new NoteWikiTitleCache();
    row.setNote(carrier);
    row.setTargetNote(focal);
    row.setLinkText("MainNb:Focal");
    noteWikiTitleCacheRepository.save(row);

    NoteRealm realm = noteRealmService.build(focal, ownerFocal);

    assertThat(realm.getReferences(), empty());
  }

  @Test
  void references_omit_soft_deleted_carrier_even_if_cache_row_remains() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Note focal = makeMe.aNote().title("Focal").underSameNotebookAs(root).please();
    Note carrier = makeMe.aNote().underSameNotebookAs(root).content("[[Focal]]").please();
    wikiTitleCacheService.refreshForNote(carrier, user);

    carrier.setDeletedAt(new Timestamp(System.currentTimeMillis()));
    makeMe.entityPersister.merge(carrier);

    NoteRealm realm = noteRealmService.build(focal, user);

    assertThat(realm.getReferences(), empty());
  }

  @Test
  void references_dedupe_multiple_cache_rows_for_same_carrier_note() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Note focal = makeMe.aNote().title("Focal").creator(user).underSameNotebookAs(root).please();
    Note carrier = makeMe.aNote().creator(user).underSameNotebookAs(root).please();

    NoteWikiTitleCache rowA = new NoteWikiTitleCache();
    rowA.setNote(carrier);
    rowA.setTargetNote(focal);
    rowA.setLinkText("one");
    NoteWikiTitleCache rowB = new NoteWikiTitleCache();
    rowB.setNote(carrier);
    rowB.setTargetNote(focal);
    rowB.setLinkText("two");
    noteWikiTitleCacheRepository.save(rowA);
    noteWikiTitleCacheRepository.save(rowB);

    NoteRealm realm = noteRealmService.build(focal, user);

    assertThat(realm.getReferences(), hasSize(1));
    assertThat(realm.getReferences().get(0).getId(), equalTo(carrier.getId()));
  }

  @Test
  void ancestor_folders_ordered_outermost_to_innermost() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Folder outer = makeMe.aFolder().notebook(root.getNotebook()).name("Outer").please();
    Folder inner =
        makeMe.aFolder().notebook(root.getNotebook()).parentFolder(outer).name("Inner").please();
    Note inFolder = makeMe.aNote().creatorAndOwner(user).folder(inner).please();

    NoteRealm realm = noteRealmService.build(inFolder, user);

    assertThat(realm.getAncestorFolders(), hasSize(2));
    assertThat(realm.getAncestorFolders().get(0).getName(), equalTo("Outer"));
    assertThat(realm.getAncestorFolders().get(1).getName(), equalTo("Inner"));
    assertThat(realm.getAncestorFolders().get(0).getId(), equalTo(outer.getId()));
    assertThat(realm.getAncestorFolders().get(1).getId(), equalTo(inner.getId()));
  }

  @Test
  void ancestor_folders_empty_when_note_not_in_folder() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();

    NoteRealm realm = noteRealmService.build(root, user);

    assertThat(realm.getAncestorFolders(), empty());
  }

  @Test
  void index_note_content_from_notebook_index_when_note_at_root_and_index_has_title_pattern() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Notebook nb = root.getNotebook();
    Note index =
        makeMe
            .aNote()
            .creatorAndOwner(user)
            .inNotebook(nb)
            .title("index")
            .content("---\ntitle_pattern: \"{{date}}\"\n---\n")
            .please();
    makeMe.theNotebook(nb).indexNote(index).please();
    Note normal = makeMe.aNote().creatorAndOwner(user).underSameNotebookAs(root).please();

    NoteRealm realm = noteRealmService.build(normal, user);

    assertThat(realm.getIndexNoteContent(), equalTo(index.getContent()));
  }

  @Test
  void index_note_content_recognizes_legacy_camel_case_title_pattern_key() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Notebook nb = root.getNotebook();
    Note index =
        makeMe
            .aNote()
            .creatorAndOwner(user)
            .inNotebook(nb)
            .title("index")
            .content("---\ntitlePattern: \"{{date}}\"\n---\n")
            .please();
    makeMe.theNotebook(nb).indexNote(index).please();
    Note normal = makeMe.aNote().creatorAndOwner(user).underSameNotebookAs(root).please();

    NoteRealm realm = noteRealmService.build(normal, user);

    assertThat(realm.getIndexNoteContent(), equalTo(index.getContent()));
  }

  @Test
  void index_note_content_prefers_inner_folder_index_title_pattern() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Notebook nb = root.getNotebook();
    Note nbIndex =
        makeMe
            .aNote()
            .creatorAndOwner(user)
            .inNotebook(nb)
            .title("index")
            .content("---\ntitle_pattern: \"nb\"\n---\n")
            .please();
    makeMe.theNotebook(nb).indexNote(nbIndex).please();

    Folder outer = makeMe.aFolder().notebook(nb).name("Outer").please();
    Note outerIdx =
        makeMe
            .aNote()
            .creatorAndOwner(user)
            .folder(outer)
            .title("index")
            .content("---\ntitle_pattern: \"outer\"\n---\n")
            .please();
    makeMe.theFolder(outer).indexNote(outerIdx).please();

    Folder inner = makeMe.aFolder().notebook(nb).parentFolder(outer).name("Inner").please();
    Note innerIdx =
        makeMe
            .aNote()
            .creatorAndOwner(user)
            .folder(inner)
            .title("index")
            .content("---\ntitle_pattern: \"inner\"\n---\n")
            .please();
    makeMe.theFolder(inner).indexNote(innerIdx).please();

    Note inInner = makeMe.aNote().creatorAndOwner(user).folder(inner).please();

    NoteRealm realm = noteRealmService.build(inInner, user);

    assertThat(realm.getIndexNoteContent(), equalTo(innerIdx.getContent()));
  }

  @Test
  void index_note_content_skips_inner_when_title_pattern_blank_and_uses_parent_folder_index() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Notebook nb = root.getNotebook();

    Folder outer = makeMe.aFolder().notebook(nb).name("Outer").please();
    Note outerIdx =
        makeMe
            .aNote()
            .creatorAndOwner(user)
            .folder(outer)
            .title("index")
            .content("---\ntitle_pattern: \"outer\"\n---\n")
            .please();
    makeMe.theFolder(outer).indexNote(outerIdx).please();

    Folder inner = makeMe.aFolder().notebook(nb).parentFolder(outer).name("Inner").please();
    Note innerIdx =
        makeMe
            .aNote()
            .creatorAndOwner(user)
            .folder(inner)
            .title("index")
            .content("---\nother: x\n---\n")
            .please();
    makeMe.theFolder(inner).indexNote(innerIdx).please();

    Note inInner = makeMe.aNote().creatorAndOwner(user).folder(inner).please();

    NoteRealm realm = noteRealmService.build(inInner, user);

    assertThat(realm.getIndexNoteContent(), equalTo(outerIdx.getContent()));
  }

  @Test
  void index_note_content_from_notebook_when_folder_index_has_no_title_pattern() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Notebook nb = root.getNotebook();
    Note nbIndex =
        makeMe
            .aNote()
            .creatorAndOwner(user)
            .inNotebook(nb)
            .title("index")
            .content("---\ntitle_pattern: \"nb\"\n---\n")
            .please();
    makeMe.theNotebook(nb).indexNote(nbIndex).please();

    Folder folder = makeMe.aFolder().notebook(nb).please();
    Note folderIdx =
        makeMe
            .aNote()
            .creatorAndOwner(user)
            .folder(folder)
            .title("index")
            .content("---\n---\n")
            .please();
    makeMe.theFolder(folder).indexNote(folderIdx).please();

    Note inFolder = makeMe.aNote().creatorAndOwner(user).folder(folder).please();

    NoteRealm realm = noteRealmService.build(inFolder, user);

    assertThat(realm.getIndexNoteContent(), equalTo(nbIndex.getContent()));
  }

  @Test
  void index_note_content_null_when_no_scoped_index_has_title_pattern() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Note normal = makeMe.aNote().creatorAndOwner(user).underSameNotebookAs(root).please();

    NoteRealm realm = noteRealmService.build(normal, user);

    assertThat(realm.getIndexNoteContent(), nullValue());
  }
}
