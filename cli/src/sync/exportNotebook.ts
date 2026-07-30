/**
 * Fetching a notebook as an exported Markdown workspace.
 *
 * The backend builds this as a zip (`NotebookExportService.exportNotebookAsZip`,
 * served by `GET /api/notebooks/{notebook}/export`), and `/export` and
 * `/sync --dry-run` share this one shape for it: `/export` writes it to disk
 * (`writeNotebookExport`), `/sync --dry-run` compares it against a workspace
 * (`previewPull`) without writing anything.
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
