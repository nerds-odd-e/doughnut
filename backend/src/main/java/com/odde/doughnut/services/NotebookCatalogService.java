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
import com.odde.doughnut.entities.repositories.BookRepository;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class NotebookCatalogService {

  private final BookRepository bookRepository;

  public NotebookCatalogService(BookRepository bookRepository) {
    this.bookRepository = bookRepository;
  }

  /**
   * Sort keys for the notebooks page catalog (merged list of ungrouped notebooks and groups):
   *
   * <ul>
   *   <li><b>Notebook</b> row: head note {@link Note#getCreatedAt()} when present, else {@link
   *       Notebook#getUpdated_at()}.
   *   <li><b>Group</b> row: {@link NotebookGroup#getCreatedAt()}.
   * </ul>
   */
  public NotebooksViewedByUser buildView(
      List<Notebook> allNotebooks, List<NotebookGroup> groups, List<Subscription> subscriptions) {
    NotebooksViewedByUser dto = new NotebooksViewedByUser();
    dto.notebooks = allNotebooks;
    dto.catalogItems = buildCatalogItems(allNotebooks, groups, subscriptions);
    fillHasAttachedBook(dto, subscriptions);
    return dto;
  }

  private void fillHasAttachedBook(NotebooksViewedByUser dto, List<Subscription> subscriptions) {
    Set<Integer> ids = new HashSet<>();
    dto.notebooks.forEach(n -> ids.add(n.getId()));
    for (Subscription s : subscriptions) {
      if (s.getNotebook() != null) {
        ids.add(s.getNotebook().getId());
      }
    }
    Set<Integer> withBook = new HashSet<>();
    if (!ids.isEmpty()) {
      withBook.addAll(bookRepository.findNotebookIdsWithAttachedBooksIn(ids));
    }
    for (Notebook n : dto.notebooks) {
      n.setHasAttachedBook(withBook.contains(n.getId()));
    }
    for (Subscription s : subscriptions) {
      Notebook nb = s.getNotebook();
      if (nb != null) {
        nb.setHasAttachedBook(withBook.contains(nb.getId()));
      }
    }
    for (NotebookCatalogItem item : dto.catalogItems) {
      switch (item) {
        case NotebookCatalogNotebookItem ni ->
            ni.notebook.setHasAttachedBook(withBook.contains(ni.notebook.getId()));
        case NotebookCatalogSubscribedNotebookItem si ->
            si.notebook.setHasAttachedBook(withBook.contains(si.notebook.getId()));
        case NotebookCatalogGroupItem gi ->
            gi.notebooks.forEach(n -> n.setHasAttachedBook(withBook.contains(n.getId())));
      }
    }
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
              catalogSortTimestamp(notebook),
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
              catalogSortTimestamp(notebook),
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
                  Comparator.comparing(NotebookCatalogService::catalogSortTimestamp)
                      .thenComparing(Notebook::getId))
              .toList();
      List<Notebook> subscribedMembers =
          subscriptions.stream()
              .filter(
                  s ->
                      s.getNotebookGroup() != null
                          && Objects.equals(s.getNotebookGroup().getId(), group.getId()))
              .sorted(
                  Comparator.comparing((Subscription s) -> catalogSortTimestamp(s.getNotebook()))
                      .thenComparing(s -> s.getNotebook().getId()))
              .map(Subscription::getNotebook)
              .toList();
      List<Notebook> allMembers = new ArrayList<>(members);
      allMembers.addAll(subscribedMembers);
      allMembers.sort(
          Comparator.comparing(NotebookCatalogService::catalogSortTimestamp)
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

  private static Timestamp catalogSortTimestamp(Notebook notebook) {
    Note head = notebook.getHeadNote();
    return head != null ? head.getCreatedAt() : notebook.getUpdated_at();
  }
}
