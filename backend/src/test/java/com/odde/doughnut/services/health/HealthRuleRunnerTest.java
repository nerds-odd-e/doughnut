package com.odde.doughnut.services.health;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.controllers.dto.HealthFindingGroup;
import com.odde.doughnut.controllers.dto.HealthFindingItem;
import com.odde.doughnut.controllers.dto.HealthSeverity;
import com.odde.doughnut.controllers.dto.NotebookHealthLintReport;
import java.util.List;
import org.junit.jupiter.api.Test;

class HealthRuleRunnerTest {

  @Test
  void nestedFindingGroupRetainsItemsAndChildren() {
    HealthFindingItem item = new HealthFindingItem();
    item.setFolderId(10);
    item.setNoteId(20);
    item.setLabel("orphan folder");
    item.setMessage("Folder has no notes and no readmeContent");
    item.setWikiLinkToken("[[Missing Target]]");

    HealthFindingGroup child = new HealthFindingGroup();
    child.setRuleId("dead_wiki_links");
    child.setTitle("Dead wiki links in note");
    child.setSeverity(HealthSeverity.warning);
    child.setAutoFixable(false);
    child.setItems(List.of());
    child.setChildren(List.of());

    HealthFindingGroup parent = new HealthFindingGroup();
    parent.setRuleId("empty_folders");
    parent.setTitle("Empty folders");
    parent.setSeverity(HealthSeverity.error);
    parent.setAutoFixable(true);
    parent.setItems(List.of(item));
    parent.setChildren(List.of(child));

    NotebookHealthLintReport report = new NotebookHealthLintReport();
    report.setGroups(List.of(parent));

    assertThat(report.getGroups(), hasSize(1));
    HealthFindingGroup retained = report.getGroups().get(0);
    assertThat(retained.getItems(), is(not(nullValue())));
    assertThat(retained.getItems(), hasSize(1));
    assertThat(retained.getItems().get(0).getFolderId(), equalTo(10));
    assertThat(retained.getItems().get(0).getNoteId(), equalTo(20));
    assertThat(retained.getItems().get(0).getLabel(), equalTo("orphan folder"));
    assertThat(
        retained.getItems().get(0).getMessage(),
        equalTo("Folder has no notes and no readmeContent"));
    assertThat(retained.getItems().get(0).getWikiLinkToken(), equalTo("[[Missing Target]]"));

    assertThat(retained.getChildren(), is(not(nullValue())));
    assertThat(retained.getChildren(), hasSize(1));
    assertThat(retained.getChildren().get(0).getRuleId(), equalTo("dead_wiki_links"));
    assertThat(retained.getChildren().get(0).getItems(), empty());

    assertThat(
        List.of(HealthSeverity.error, HealthSeverity.warning, HealthSeverity.info),
        contains(HealthSeverity.error, HealthSeverity.warning, HealthSeverity.info));
    assertThat(retained.getSeverity(), equalTo(HealthSeverity.error));
    assertThat(retained.getChildren().get(0).getSeverity(), equalTo(HealthSeverity.warning));
  }
}
