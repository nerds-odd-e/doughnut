/**
 * Writing a notebook out as a Markdown workspace.
 *
 * The preview needs a copy of the notebook as it stands to compare a workspace
 * against. Another team owns exporting; they are building the web side first,
 * so this is the seam we agreed to define from our end and reconcile with
 * theirs once their command exists. See
 * `docs/refinement/2026-07-27/QUESTIONS-for-export-team.md`.
 */
export type ExportNotebook = (
  notebookId: number,
  targetDirectory: string,
  signal?: AbortSignal
) => Promise<void>

/**
 * The export that is not available yet. Wiring this in place of a real one
 * keeps the preview honest: it reports that exporting is unavailable rather
 * than pretending a notebook has no notes.
 */
export const exportNotebookUnavailable: ExportNotebook = () => {
  return Promise.reject(new Error('Exporting a notebook is not available yet.'))
}
