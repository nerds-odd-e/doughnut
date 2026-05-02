package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteWikiTitleCache;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteWikiTitleCacheRepository;
import java.sql.Timestamp;
import java.util.List;
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
  void wiki_titles_empty_when_details_have_links_but_cache_not_refreshed() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    makeMe.aNote().title("LinkedPage").under(root).please();
    Note carrier = makeMe.aNote().under(root).details("[[LinkedPage]]").please();

    NoteRealm realm = noteRealmService.build(carrier, user);

    assertThat(realm.getWikiTitles(), empty());
  }

  @Test
  void omits_cached_target_when_viewer_cannot_read_target_notebook() {
    User otherUser = makeMe.aUser().please();
    Note headSecret = makeMe.aNote().creatorAndOwner(otherUser).title("SecretNb").please();
    Note hidden = makeMe.aNote().title("Hidden").under(headSecret).please();

    User viewer = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(viewer).please();
    Note carrier = makeMe.aNote().under(root).details("plain").please();

    NoteWikiTitleCache row = new NoteWikiTitleCache();
    row.setNote(carrier);
    row.setTargetNote(hidden);
    row.setLinkText("SecretNb:Hidden");
    noteWikiTitleCacheRepository.save(row);

    NoteRealm realm = noteRealmService.build(carrier, viewer);

    assertThat(realm.getWikiTitles(), empty());
  }

  @Test
  void inbound_references_use_cache_rows_pointing_at_focal_same_notebook() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Note focal = makeMe.aNote().title("Focal").under(root).please();
    Note carrier = makeMe.aNote().under(root).details("[[Focal]]").please();
    wikiTitleCacheService.refreshForNote(carrier, user);

    NoteRealm realm = noteRealmService.build(focal, user);

    assertThat(realm.getInboundReferences(), hasSize(1));
    assertThat(realm.getInboundReferences().get(0).getId(), equalTo(carrier.getId()));
    assertThat(realm.getReferences(), hasSize(1));
    assertThat(realm.getReferences().get(0).getId(), equalTo(carrier.getId()));
  }

  @Test
  void inbound_empty_when_cache_not_refreshed_even_if_legacy_target_fk_exists() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Note focal = makeMe.aNote().title("Focal").under(root).please();
    Note subject = makeMe.aNote().under(root).please();
    makeMe.aRelation().between(subject, focal).please();

    NoteRealm realm = noteRealmService.build(focal, user);

    assertThat(realm.getInboundReferences(), empty());
    assertThat(realm.getReferences(), empty());
  }

  @Test
  void subject_realm_references_include_relation_note_when_cache_refreshed() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Note focal = makeMe.aNote().title("Focal").under(root).please();
    Note subject = makeMe.aNote().under(root).please();
    Note relation = makeMe.aRelation().between(subject, focal).please();
    relation.setDetails(
        RelationshipNoteMarkdownFormatter.formatForRelationshipNote(
            relation, relation.getRelationType(), subject, focal, null));
    makeMe.entityPersister.merge(relation);
    makeMe.entityPersister.flush();
    wikiTitleCacheService.refreshForNote(relation, user);

    NoteRealm subjectRealm = noteRealmService.build(subject, user);

    assertThat(subjectRealm.getReferences(), hasSize(1));
    assertThat(subjectRealm.getReferences().get(0).getId(), equalTo(relation.getId()));

    assertThat(
        wikiTitleCacheService.subjectAndParentLinkedReferrerNotesForViewer(focal, user), empty());
  }

  @Test
  void references_empty_when_cache_not_refreshed_even_if_structural_child_exists() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Note focal = makeMe.aNote().title("Focal").under(root).please();
    Note subject = makeMe.aNote().under(root).please();
    makeMe.aRelation().between(subject, focal).please();

    NoteRealm realm = noteRealmService.build(subject, user);

    assertThat(realm.getReferences(), empty());
  }

  @Test
  void body_wikilink_carrier_is_inbound_only_not_subject_parent_slice() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Note focal = makeMe.aNote().title("Focal").under(root).please();
    Note carrier = makeMe.aNote().under(root).details("[[Focal]]").please();
    wikiTitleCacheService.refreshForNote(carrier, user);

    NoteRealm realm = noteRealmService.build(focal, user);

    assertThat(realm.getInboundReferences(), hasSize(1));
    assertThat(realm.getInboundReferences().get(0).getId(), equalTo(carrier.getId()));
    assertThat(
        wikiTitleCacheService.subjectAndParentLinkedReferrerNotesForViewer(focal, user), empty());
    assertThat(realm.getReferences(), hasSize(1));
    assertThat(realm.getReferences().get(0).getId(), equalTo(carrier.getId()));
  }

  @Test
  void parent_yaml_carrier_appears_in_inbound_and_references() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Note focal = makeMe.aNote().title("Focal").under(root).please();
    Note carrier = makeMe.aNote().title("Child").under(root).please();
    carrier.setDetails("---\nparent: \"[[Focal]]\"\n---\n\nBody.");
    makeMe.entityPersister.merge(carrier);
    makeMe.entityPersister.flush();
    wikiTitleCacheService.refreshForNote(carrier, user);

    NoteRealm realm = noteRealmService.build(focal, user);

    assertThat(realm.getInboundReferences(), hasSize(1));
    assertThat(realm.getReferences(), hasSize(1));
    assertThat(realm.getReferences().get(0).getId(), equalTo(carrier.getId()));
  }

  @Test
  void references_omit_soft_deleted_relation_even_if_cache_row_remains() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Note focal = makeMe.aNote().title("Focal").under(root).please();
    Note subject = makeMe.aNote().under(root).please();
    Note relation = makeMe.aRelation().between(subject, focal).please();
    relation.setDetails(
        RelationshipNoteMarkdownFormatter.formatForRelationshipNote(
            relation, relation.getRelationType(), subject, focal, null));
    makeMe.entityPersister.merge(relation);
    makeMe.entityPersister.flush();
    wikiTitleCacheService.refreshForNote(relation, user);

    relation.setDeletedAt(new Timestamp(System.currentTimeMillis()));
    makeMe.entityPersister.merge(relation);

    NoteRealm realm = noteRealmService.build(subject, user);

    assertThat(realm.getReferences(), empty());
  }

  @Test
  void inbound_includes_cross_notebook_carrier_when_viewer_can_refer() {
    User user = makeMe.aUser().please();
    Note headMain = makeMe.aNote().creatorAndOwner(user).title("MainNb").please();
    Note focal = makeMe.aNote().title("Focal").under(headMain).please();
    Note headOther = makeMe.aNote().creatorAndOwner(user).title("OtherNb").please();
    Note carrier = makeMe.aNote().under(headOther).please();

    NoteWikiTitleCache row = new NoteWikiTitleCache();
    row.setNote(carrier);
    row.setTargetNote(focal);
    row.setLinkText("MainNb:Focal");
    noteWikiTitleCacheRepository.save(row);

    NoteRealm realm = noteRealmService.build(focal, user);

    assertThat(realm.getInboundReferences(), hasSize(1));
    assertThat(realm.getInboundReferences().get(0).getId(), equalTo(carrier.getId()));
    assertThat(realm.getReferences(), hasSize(1));
    assertThat(realm.getReferences().get(0).getId(), equalTo(carrier.getId()));
  }

  @Test
  void inbound_omits_cross_notebook_carrier_when_viewer_cannot_refer() {
    User ownerFocal = makeMe.aUser().please();
    User ownerCarrier = makeMe.aUser().please();
    Note headMain = makeMe.aNote().creatorAndOwner(ownerFocal).title("MainNb").please();
    Note focal = makeMe.aNote().title("Focal").under(headMain).please();
    Note headOther = makeMe.aNote().creatorAndOwner(ownerCarrier).title("OtherNb").please();
    Note carrier = makeMe.aNote().under(headOther).please();

    NoteWikiTitleCache row = new NoteWikiTitleCache();
    row.setNote(carrier);
    row.setTargetNote(focal);
    row.setLinkText("MainNb:Focal");
    noteWikiTitleCacheRepository.save(row);

    NoteRealm realm = noteRealmService.build(focal, ownerFocal);

    assertThat(realm.getInboundReferences(), empty());
    assertThat(realm.getReferences(), empty());
  }

  @Test
  void inbound_omits_soft_deleted_carrier_even_if_cache_row_remains() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Note focal = makeMe.aNote().title("Focal").under(root).please();
    Note carrier = makeMe.aNote().under(root).details("[[Focal]]").please();
    wikiTitleCacheService.refreshForNote(carrier, user);

    carrier.setDeletedAt(new Timestamp(System.currentTimeMillis()));
    makeMe.entityPersister.merge(carrier);

    NoteRealm realm = noteRealmService.build(focal, user);

    assertThat(realm.getInboundReferences(), empty());
    assertThat(realm.getReferences(), empty());
  }

  @Test
  void inbound_dedupes_multiple_cache_rows_for_same_carrier_note() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Note focal = makeMe.aNote().title("Focal").under(root).please();
    Note carrier = makeMe.aNote().under(root).please();

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

    assertThat(realm.getInboundReferences(), hasSize(1));
    assertThat(realm.getInboundReferences().get(0).getId(), equalTo(carrier.getId()));
    assertThat(realm.getReferences(), hasSize(1));
    assertThat(realm.getReferences().get(0).getId(), equalTo(carrier.getId()));
  }

  @Test
  void merge_reference_notes_dedupes_and_orders_by_note_id() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Note second = makeMe.aNote().under(root).please();
    Note first = makeMe.aNote().under(root).please();

    List<Note> merged =
        NoteRealmService.mergeReferenceNotes(List.of(second), List.of(first, second));
    assertThat(merged, hasSize(2));
    assertThat(merged.get(0).getId(), equalTo(Math.min(first.getId(), second.getId())));
    assertThat(merged.get(1).getId(), equalTo(Math.max(first.getId(), second.getId())));
  }

  @Test
  void ancestor_folders_ordered_outermost_to_innermost() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();
    Folder outer = makeMe.aFolder().notebook(root.getNotebook()).name("Outer").please();
    Folder inner =
        makeMe.aFolder().notebook(root.getNotebook()).parentFolder(outer).name("Inner").please();
    Note inFolder = makeMe.aNote().creatorAndOwner(user).under(root).folder(inner).please();

    NoteRealm realm = noteRealmService.build(inFolder, user);

    assertThat(realm.getAncestorFolders(), hasSize(2));
    assertThat(realm.getAncestorFolders().get(0).name(), equalTo("Outer"));
    assertThat(realm.getAncestorFolders().get(1).name(), equalTo("Inner"));
    assertThat(realm.getAncestorFolders().get(0).id(), equalTo(outer.getId()));
    assertThat(realm.getAncestorFolders().get(1).id(), equalTo(inner.getId()));
  }

  @Test
  void ancestor_folders_empty_when_note_not_in_folder() {
    User user = makeMe.aUser().please();
    Note root = makeMe.aNote().creatorAndOwner(user).please();

    NoteRealm realm = noteRealmService.build(root, user);

    assertThat(realm.getAncestorFolders(), empty());
  }
}
