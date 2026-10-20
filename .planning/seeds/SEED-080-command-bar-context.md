# Sidebar location context

Supporting dated evidence for [SEED-080](SEED-080-command-bar-reconciliation.md#story-2).

Sidebar follow-up inspected on 2026-10-20 at
`428dfdf574f687ec83178f15ab4f07b36f315483`, after the owner selected the
“Sidebar header” alternative. This is source/history inspection, not a
runtime layout observation.

- `NotebookSidebarLayout.vue` currently derives notebook and folder location
  for notes, folders, and attachments. It puts desktop breadcrumbs beside the
  note toolbar and phone breadcrumbs in a separate row. `Sidebar.vue` renders
  its commands below that global bar. These are the two layout responsibilities
  to reconcile, using the same domain context.
- The scroll-dependent ancestor display and its exclusive support were
  deleted by `56505b78dd934aa2efe8dc32ddd5564e913ffeae` on 2026-09-28.
  A one-time reading at the inspected revision found no surviving exact-name
  or display-text references across frontend, E2E, docs, agent guidance, and
  planning. Recover the deleted identifiers from that commit when repeating
  the acceptance sweep, rather than introducing a permanent checker.
- `SidebarFolderItem`, `useSidebarRowReveal`, and the existing sidebar
  reveal/loading/route tests own the retained expansion, selection, row
  visibility, and pending-load behavior. They are reconciliation assets.

