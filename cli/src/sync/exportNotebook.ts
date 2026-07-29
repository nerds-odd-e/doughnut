/**
 * Fetching a notebook as an exported Markdown workspace.
 *
 * The preview needs a copy of the notebook as it stands to compare a workspace
 * against. The backend builds that copy as a zip
 * (`NotebookExportService.exportNotebookAsZip`), which another team owns; they
 * are building the web side first, so this is the seam we define from our end
 * and reconcile with theirs once the endpoint exists. See
 * `docs/refinement/2026-07-27/QUESTIONS-for-export-team.md`.
 */
export type NotebookExport = {
  readonly bytes: Buffer
  /** As the backend named it, e.g. `Ben Notebook.zip`. */
  readonly fileName: string
}

export type ExportNotebookAsZip = (
  notebookId: number,
  signal?: AbortSignal
) => Promise<NotebookExport>
