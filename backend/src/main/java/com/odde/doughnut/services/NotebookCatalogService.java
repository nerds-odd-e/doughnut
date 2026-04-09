package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.NotebookCatalogGroupItem;
import com.odde.doughnut.controllers.dto.NotebookCatalogItem;
import com.odde.doughnut.controllers.dto.NotebookCatalogNotebookItem;
import com.odde.doughnut.controllers.dto.NotebookCatalogSubscribedNotebookItem;
import com.odde.doughnut.controllers.dto.NotebooksViewedByUser;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookGroup;
import com.odde.doughnut.entities.Subscription;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class NotebookCatalogService {

  /**
   * Sort keys for the notebooks page catalog (merged list of ungrouped notebooks and groups):
   *
   * <ul>
   *   <li><b>Notebook</b> row: {@link Notebook#getHeadNote()} {@link Note#getCreatedAt()} — there
   *       is no {@code notebook.created_at} column.
   *   <li><b>Group</b> row: {@link NotebookGroup#getCreatedAt()}.
   * </ul>
   *
   * <p>Members inside a group row use the same head-note {@code created_at} ordering.
   */
  public NotebooksViewedByUser buildView(
      List<Notebook> allNotebooks, List<NotebookGroup> groups, List<Subscription> subscriptions) {
    NotebooksViewedByUser dto = new NotebooksViewedByUser();
    dto.notebooks = allNotebooks;
    dto.catalogItems = buildCatalogItems(allNotebooks, groups, subscriptions);
    return dto;
  }

  private static List<NotebookCatalogItem> buildCatalogItems(
      List<Notebook> allNotebooks, List<NotebookGroup> groups, List<Subscription> subscriptions) {
    List<SortableRow> rows = new ArrayList<>();

    for (Notebook notebook : allNotebooks) {
      if (notebook.getNotebookGroup() != null) {
        continue;
      }
      rows.add(
          new SortableRow(
              new NotebookCatalogNotebookItem(notebook),
              notebook.getHeadNote().getCreatedAt(),
              0,
              notebook.getId()));
    }

    for (Subscription subscription : subscriptions) {
      if (subscription.getNotebookGroup() != null) {
        continue;
      }
      Notebook notebook = subscription.getNotebook();
      rows.add(
          new SortableRow(
              new NotebookCatalogSubscribedNotebookItem(notebook, subscription.getId()),
              notebook.getHeadNote().getCreatedAt(),
              0,
              notebook.getId()));
    }

    for (NotebookGroup group : groups) {
      List<Notebook> members =
          allNotebooks.stream()
              .filter(
                  nb ->
                      nb.getNotebookGroup() != null
                          && Objects.equals(nb.getNotebookGroup().getId(), group.getId()))
              .sorted(
                  Comparator.comparing((Notebook nb) -> nb.getHeadNote().getCreatedAt())
                      .thenComparing(Notebook::getId))
              .toList();
      List<Notebook> subscribedMembers =
          subscriptions.stream()
              .filter(
                  s ->
                      s.getNotebookGroup() != null
                          && Objects.equals(s.getNotebookGroup().getId(), group.getId()))
              .sorted(
                  Comparator.comparing(
                          (Subscription s) -> s.getNotebook().getHeadNote().getCreatedAt())
                      .thenComparing(s -> s.getNotebook().getId()))
              .map(Subscription::getNotebook)
              .toList();
      List<Notebook> allMembers = new ArrayList<>(members);
      allMembers.addAll(subscribedMembers);
      allMembers.sort(
          Comparator.comparing((Notebook nb) -> nb.getHeadNote().getCreatedAt())
              .thenComparing(Notebook::getId));
      rows.add(
          new SortableRow(
              new NotebookCatalogGroupItem(
                  group.getId(), group.getName(), group.getCreatedAt(), allMembers),
              group.getCreatedAt(),
              1,
              group.getId()));
    }

    rows.sort(
        Comparator.comparing((SortableRow r) -> r.sortAt)
            .thenComparing(r -> r.kind)
            .thenComparing(r -> r.tieId));

    return rows.stream().map(r -> r.item).toList();
  }

  private record SortableRow(NotebookCatalogItem item, Timestamp sortAt, int kind, int tieId) {}
}
