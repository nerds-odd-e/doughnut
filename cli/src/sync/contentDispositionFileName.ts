/**
 * Read the filename a `Content-Disposition` header names.
 *
 * The backend already decides what a notebook's export is called
 * (`NotebookExportService.exportFileName`, which sanitizes the notebook name),
 * so reading it here keeps that rule in one place instead of mirroring it.
 */
export function contentDispositionFileName(
  header: string | null | undefined
): string | undefined {
  if (header === null || header === undefined) return
  const quoted = /;\s*filename\s*=\s*"([^"]*)"/i.exec(header)
  if (quoted !== null) return quoted[1]
  const unquoted = /;\s*filename\s*=\s*([^;\s]+)/i.exec(header)
  if (unquoted !== null) return unquoted[1]
  return
}
