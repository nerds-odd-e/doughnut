package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.DisplayName;
import com.odde.donut.entities.EntityIdentifiedByIdOnly;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.hibernate.Interceptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.type.Type;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

/**
 * Records, while a capture window is open on the current thread, which projection rows (notes,
 * folders, attachments, notebook readmes) the flushed changes inserted, updated or deleted, grouped
 * by notebook. A row whose notebook changed is deleted from the previous notebook and inserted into
 * the current one. Updates keep the first-seen previous path fields so a later flush cannot lose
 * the original path; deletes keep the path the row had, since the entity is gone. Attachment bytes
 * are never read. The window is not reentrant: opening one replaces any window already open on the
 * thread, so the accepted-change owner must not re-enter itself.
 */
@Component
class ProjectionChangeCapture implements Interceptor, HibernatePropertiesCustomizer {

  record ProjectionRow(Class<?> kind, Integer id) {}

  /** Container is the folder (notes, attachments) or parent folder (folders); null at root. */
  record RowPath(Integer containerId, String name) {}

  static class NotebookProjectionChange {
    /** The inserted entities themselves: a row's id is only assigned by its insert. */
    final Set<Object> inserted = new LinkedHashSet<>();

    final Map<ProjectionRow, RowPath> updated = new LinkedHashMap<>();
    final Map<ProjectionRow, RowPath> deleted = new LinkedHashMap<>();
  }

  class ProjectionChange implements AutoCloseable {
    private final Map<Integer, NotebookProjectionChange> byNotebook = new HashMap<>();

    NotebookProjectionChange of(Integer notebookId) {
      return byNotebook.computeIfAbsent(notebookId, id -> new NotebookProjectionChange());
    }

    @Override
    public void close() {
      window.remove();
    }
  }

  /** Property names of a row's path fields: its container and its display name. */
  private record PathFields(String container, String name) {}

  private static final Map<Class<?>, PathFields> PATH_FIELDS =
      Map.of(
          Note.class, new PathFields("folder", "title"),
          Folder.class, new PathFields("parentFolder", "name"),
          NotebookAttachment.class, new PathFields("folder", "filename"));

  private final ThreadLocal<ProjectionChange> window = new ThreadLocal<>();

  ProjectionChange open() {
    ProjectionChange change = new ProjectionChange();
    window.set(change);
    return change;
  }

  @Override
  public void customize(Map<String, Object> hibernateProperties) {
    hibernateProperties.put(AvailableSettings.INTERCEPTOR, this);
  }

  @Override
  public boolean onSave(Object entity, Object id, Object[] state, String[] names, Type[] types) {
    NotebookProjectionChange change = changeFor(entity, state, names);
    if (change != null) change.inserted.add(entity);
    return false;
  }

  @Override
  public void onDelete(Object entity, Object id, Object[] state, String[] names, Type[] types) {
    NotebookProjectionChange change = changeFor(entity, state, names);
    if (change != null) change.deleted.put(row(entity, id), rowPath(entity, state, names));
  }

  @Override
  public boolean onFlushDirty(
      Object entity, Object id, Object[] current, Object[] previous, String[] names, Type[] types) {
    NotebookProjectionChange change = changeFor(entity, previous, names);
    if (change == null) return false;
    ProjectionRow row = row(entity, id);
    RowPath previousPath = previousPath(entity, current, previous, names);
    if (leftNotebook(entity, current, previous, names)) {
      RowPath firstSeen = change.updated.remove(row);
      change.deleted.put(row, Objects.requireNonNullElse(firstSeen, previousPath));
      changeFor(entity, current, names).inserted.add(entity);
    } else if (previousPath != null) change.updated.putIfAbsent(row, previousPath);
    return false;
  }

  /** A moved row leaves its previous notebook and arrives in its current one. */
  private static boolean leftNotebook(
      Object entity, Object[] current, Object[] previous, String[] names) {
    return !(entity instanceof Notebook)
        && !Objects.equals(idAt(previous, names, "notebook"), idAt(current, names, "notebook"));
  }

  /** The window's record for the entity's notebook; null when no window is open or not tracked. */
  private NotebookProjectionChange changeFor(Object entity, Object[] state, String[] names) {
    ProjectionChange change = window.get();
    if (change == null) return null;
    if (entity instanceof Notebook notebook) return change.of(notebook.getId());
    if (!PATH_FIELDS.containsKey(entity.getClass())) return null;
    return change.of(idAt(state, names, "notebook"));
  }

  /** A notebook row counts as a path change only when its root README content changed. */
  private static RowPath previousPath(
      Object entity, Object[] current, Object[] previous, String[] names) {
    if (entity instanceof Notebook) {
      boolean readmeUnchanged =
          Objects.equals(at(previous, names, "readmeContent"), at(current, names, "readmeContent"));
      return readmeUnchanged ? null : new RowPath(null, null);
    }
    return rowPath(entity, previous, names);
  }

  private static RowPath rowPath(Object entity, Object[] state, String[] names) {
    PathFields fields = PATH_FIELDS.get(entity.getClass());
    return new RowPath(idAt(state, names, fields.container()), nameAt(state, names, fields.name()));
  }

  private static ProjectionRow row(Object entity, Object id) {
    return new ProjectionRow(entity.getClass(), (Integer) id);
  }

  private static Object at(Object[] state, String[] names, String name) {
    return state[Arrays.asList(names).indexOf(name)];
  }

  private static Integer idAt(Object[] state, String[] names, String name) {
    Object value = at(state, names, name);
    return value == null ? null : ((EntityIdentifiedByIdOnly) value).getId();
  }

  private static String nameAt(Object[] state, String[] names, String name) {
    Object value = at(state, names, name);
    return value instanceof DisplayName displayName ? displayName.value() : (String) value;
  }
}
