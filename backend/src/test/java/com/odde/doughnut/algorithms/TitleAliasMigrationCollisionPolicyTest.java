package com.odde.doughnut.algorithms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TitleAliasMigrationCollisionPolicyTest {

  @Test
  void resolve_noCollision_keepsPlannedTitles() {
    var inputs =
        List.of(
            placement(1, 10, null, "colour"),
            placement(2, 10, null, "hue"),
            placement(3, 20, null, "colour"));

    Map<Integer, String> resolved = TitleAliasMigrationCollisionPolicy.resolve(inputs);

    assertThat(resolved.get(1), equalTo("colour"));
    assertThat(resolved.get(2), equalTo("hue"));
    assertThat(resolved.get(3), equalTo("colour"));
    assertThat(TitleAliasMigrationCollisionPolicy.collisionGroups(inputs), empty());
  }

  @Test
  void resolve_sameNotebookAndFolder_lowestIdKeepsBareTitle_othersGetNumberedQualifiers() {
    var inputs =
        List.of(
            placement(2, 10, null, "colour"),
            placement(5, 10, null, "colour"),
            placement(8, 10, null, "colour"));

    Map<Integer, String> resolved = TitleAliasMigrationCollisionPolicy.resolve(inputs);

    assertThat(resolved.get(2), equalTo("colour"));
    assertThat(resolved.get(5), equalTo("colour (1)"));
    assertThat(resolved.get(8), equalTo("colour (2)"));
  }

  @Test
  void resolve_differentFoldersInSameNotebook_doNotCollide() {
    var inputs = List.of(placement(1, 10, 100, "colour"), placement(2, 10, 200, "colour"));

    Map<Integer, String> resolved = TitleAliasMigrationCollisionPolicy.resolve(inputs);

    assertThat(resolved.get(1), equalTo("colour"));
    assertThat(resolved.get(2), equalTo("colour"));
    assertThat(TitleAliasMigrationCollisionPolicy.collisionGroups(inputs), empty());
  }

  @Test
  void resolve_existingQualifier_extendsWithNumberedSuffix() {
    var inputs =
        List.of(placement(1, 10, null, "cat (animal)"), placement(2, 10, null, "cat (animal)"));

    Map<Integer, String> resolved = TitleAliasMigrationCollisionPolicy.resolve(inputs);

    assertThat(resolved.get(1), equalTo("cat (animal)"));
    assertThat(resolved.get(2), equalTo("cat (animal 1)"));
  }

  @Test
  void resolve_existingNumberedQualifier_extendsWithNumberedSuffix() {
    var inputs =
        List.of(
            placement(1, 10, null, "cat (1)"),
            placement(2, 10, null, "cat (1)"),
            placement(3, 10, null, "cat (1)"));

    Map<Integer, String> resolved = TitleAliasMigrationCollisionPolicy.resolve(inputs);

    assertThat(resolved.get(1), equalTo("cat (1)"));
    assertThat(resolved.get(2), equalTo("cat (1 1)"));
    assertThat(resolved.get(3), equalTo("cat (1 2)"));
  }

  @Test
  void collisionGroups_reportsFullCollisionListWithResolvedTitles() {
    var inputs = List.of(placement(2, 10, 50, "colour"), placement(5, 10, 50, "colour"));

    var groups = TitleAliasMigrationCollisionPolicy.collisionGroups(inputs);

    assertThat(groups, hasSize(1));
    var group = groups.getFirst();
    assertThat(group.notebookId(), equalTo(10));
    assertThat(group.folderId(), equalTo(50));
    assertThat(group.basePlannedTitle(), equalTo("colour"));
    assertThat(
        group.members().stream().map(TitleAliasMigrationCollisionPolicy.Member::noteId).toList(),
        contains(2, 5));
    assertThat(
        group.members().stream()
            .map(TitleAliasMigrationCollisionPolicy.Member::resolvedTitle)
            .toList(),
        contains("colour", "colour (1)"));
  }

  @Test
  void resolve_caseInsensitivePlannedTitles_collideWithinNotebookAndFolder() {
    var inputs = List.of(placement(11990, 4, 2697, "xAI"), placement(11991, 4, 2697, "XAI"));

    Map<Integer, String> resolved = TitleAliasMigrationCollisionPolicy.resolve(inputs);

    assertThat(resolved.get(11990), equalTo("xAI"));
    assertThat(resolved.get(11991), equalTo("XAI (1)"));
    assertThat(
        TitleAliasMigrationCollisionPolicy.collisionNoteIds(inputs),
        containsInAnyOrder(11990, 11991));
  }

  private static TitleAliasMigrationCollisionPolicy.NotePlacement placement(
      int noteId, int notebookId, Integer folderId, String basePlannedTitle) {
    return new TitleAliasMigrationCollisionPolicy.NotePlacement(
        noteId, notebookId, folderId, basePlannedTitle);
  }
}
