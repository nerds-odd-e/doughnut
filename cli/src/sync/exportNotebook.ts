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
export type ExportNotebookAsZip = (
  notebookId: number,
  signal?: AbortSignal
) => Promise<Buffer>

/**
 * The export that is not reachable yet. Wiring this in place of a real one
 * keeps the preview honest: it reports that exporting is unavailable rather
 * than pretending a notebook has no notes.
 */
export const exportNotebookAsZipUnavailable: ExportNotebookAsZip = () =>
  Promise.reject(
    new Error(
      'Exporting a notebook is not available yet. The endpoint is still being built.'
    )
  )
