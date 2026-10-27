# Notebook workspace

The notebook workspace identifies the current notebook and folder location on
note and property, folder, notebook-root and attachment pages. Notebook identity
sits above the compact folder trail. The location control opens a labeled dialog
with complete names and navigable notebook and ancestor links, including circle
or Bazaar context. Keyboard and pointer activation share the same destinations;
dismissal returns focus to the location control. Notebook roots retain their
identity with an empty folder trail.

With contents docked, location sits above the tree, outside its scrollport.
Scrolling the tree leaves location visible. With contents hidden, location is
available above main content. On phones, opening contents places location and
contents commands in the drawer, above its independently scrolling tree. The
existing note-route navigation policy closes the phone drawer.

Contents creation and sorting commands share the docked top row with the note
toolbar. The pane divider follows the sidebar edge, while note actions use the
right edge of their available allocation and existing adaptive overflow. The
sidebar toggle remains independent of active navigation. Open note actions and
recording remain usable through contents toggles and viewport changes. Creation
retains notebook-root or current-folder scope and the existing access rules.

`NotebookSidebarLayout` derives location from existing route realms and passes
that context to location, tree and creation consumers. Pending note navigation
retains the current realm through `useStickyActiveNoteRealmForRoute`.
`NotebookLocation` reuses shared breadcrumb navigation inside the standard modal;
read-only context and recall retain their own breadcrumb presentation.
`Sidebar` owns the single contents toolbar, teleporting it into the docked bar.
The layout owns the pane-width CSS variable used by both drawer and top row.
Tree expansion, listing cache, root reset and minimal active-row reveal stay with
their existing sidebar owners.

Navigation uses named locations and the shared note/property route family under
[ADR 0005](adrs/0005-web-routes-accepted.md). Folder location follows the
[notebook domain vocabulary](adrs/0001-ubiquitous-language.md#notebook--note-structure).

## Notebook catalog

The notebooks page autofocuses its filter when a non-empty catalog loads. The
input has an accessible name and the placeholder "Search by notebook or group
name". Filtering and sorting determine the entries available to keyboard
navigation in both list and grid layouts.

Down from the filter focuses the first displayed entry link. Up and Down visit
the displayed notebooks, subscriptions, group headers and visible group members
in reading order. Up from the first entry returns to the filter with its text
intact. Up at the filter, Down at the last entry, and Down with no matches keep
focus in place.

Right from a group header focuses its first visible member; Left from a member
returns to its header. An empty header and unrelated entries keep focus on
horizontal arrows. Group previews retain their displayed size. Enter activates
the focused notebook or group link, including empty and preview-limited groups.
Entry links show a focus ring and use the browser's focus scrolling.

Catalog arrows apply only to the filter and entry links. Left and Right in the
filter retain text-cursor behavior; Tab and Shift+Tab retain access to clear,
layout, sort and secondary controls. The catalog and search dialogs share the
input-to-list traversal rule, while catalog group navigation reads membership
and order from the rendered group panels.
