import { mkdirSync, writeFileSync } from 'node:fs'
import { basename, dirname, join } from 'node:path'
import type { ExportNotebookAsZip } from './exportNotebook.js'
import { savePushBaseline } from './pushBaseline.js'
import { unzipToEntries } from './unzip.js'

const ZIP_SUFFIX = '.zip'
const MARKDOWN_SUFFIX = '.md'

export type WriteNotebookExportRequest = {
  readonly notebookId: number
  /** Absolute path of the directory to write the notebook's directory into. */
  readonly destinationDirectory: string
  readonly exportNotebookAsZip: ExportNotebookAsZip
  readonly signal?: AbortSignal
}

/**
 * A zip entry path is trusted only as far as "no separator that could escape
 * `root`": absolute paths, `..` segments, and `\` (a Windows separator the
 * backend never emits, but this only unzips bytes it did not create) are all
 * rejected before anything is written.
 */
function assertSafeEntryPath(path: string): void {
  if (
    path.startsWith('/') ||
    path.includes('\\') ||
    path.split('/').includes('..')
  ) {
    throw new Error(`The export contained an unsafe path: ${path}.`)
  }
}

function render(root: string, paths: readonly string[]): string {
  const count =
    paths.length === 1 ? '1 file written.' : `${paths.length} files written.`
  return [
    `Exported to ${root}`,
    ...paths.map((path) => `  ${path}`),
    '',
    count,
  ].join('\n')
}

/**
 * Write the notebook's export into a directory of its own under
 * `destinationDirectory`.
 *
 * The notebook gets a subdirectory rather than being poured straight into the
 * destination, so exporting several notebooks into one folder keeps them apart.
 * Its name comes from the name the backend gave the download, which is already
 * sanitized for a filesystem; deriving it again here would be a second rule to
 * keep in step with the first.
 *
 * Files of the same name are overwritten. Anything else already in the
 * destination is left alone: this writes a notebook, it does not mirror one.
 *
 * This also seeds `.doughnut-sync/baseline.json`, the same baseline
 * `/push --dry-run` maintains: right after writing, every exported note's
 * workspace and Doughnut content agree by construction, so a `/push --dry-run`
 * run against an edit made right after this already has history to compare
 * against, instead of needing a priming run first.
 */
export async function writeNotebookExport({
  notebookId,
  destinationDirectory,
  exportNotebookAsZip,
  signal,
}: WriteNotebookExportRequest): Promise<string> {
  const { bytes, fileName } = await exportNotebookAsZip(notebookId, signal)
  const entries = [...unzipToEntries(bytes)].sort(([a], [b]) =>
    a < b ? -1 : a > b ? 1 : 0
  )
  if (entries.length === 0) {
    return 'Nothing to export: the notebook has no notes.'
  }

  const root = join(destinationDirectory, basename(fileName, ZIP_SUFFIX))
  for (const [path] of entries) {
    assertSafeEntryPath(path)
  }
  for (const [path, content] of entries) {
    const full = join(root, path)
    mkdirSync(dirname(full), { recursive: true })
    writeFileSync(full, content, 'utf8')
  }

  savePushBaseline(
    root,
    notebookId,
    new Map(entries.filter(([path]) => path.endsWith(MARKDOWN_SUFFIX)))
  )

  return render(
    root,
    entries.map(([path]) => path)
  )
}
