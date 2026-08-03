/**
 * What a dry-run pull would do with one exported Markdown path.
 *
 * Kept separate from push `NoteDiffStatus` (`pull` | `push` | `conflict`) so
 * `/push --dry-run` labeling stays untouched.
 */
export type PreviewPullAction = 'create' | 'update' | 'move' | 'reject'

const MARKDOWN_SUFFIX = '.md'
const RESERVED_BASENAMES = new Set(['index.md', 'log.md'])
const SYNC_METADATA_SEGMENT = '.doughnut-sync'

/** Case-insensitive `doughnut_id:` line, aligned with backend DOUGHNUT_ID_LINE. */
const DOUGHNUT_ID_LINE = /^doughnut_id\s*:\s*(.*?)\s*$/i

/**
 * Read the note id from a leading YAML fence when present. Missing or empty →
 * undefined (path-keyed create/update only; never invent a move).
 */
export function extractDoughnutId(content: string): string | undefined {
  const lines = content.replace(/\r\n/g, '\n').split('\n')
  if (lines[0] !== '---') return
  for (let i = 1; i < lines.length; i++) {
    if (lines[i] === '---') return
    const match = DOUGHNUT_ID_LINE.exec(lines[i])
    if (match) {
      const id = match[1]?.trim()
      return id === undefined || id === '' ? undefined : id
    }
  }
  return
}

/** id → first workspace path that holds that doughnut_id. */
export function indexPathsByDoughnutId(
  notes: ReadonlyMap<string, string>
): Map<string, string> {
  const byId = new Map<string, string>()
  for (const [path, content] of notes) {
    if (!path.endsWith(MARKDOWN_SUFFIX)) continue
    const id = extractDoughnutId(content)
    if (id !== undefined && !byId.has(id)) byId.set(id, path)
  }
  return byId
}

/**
 * Path-keyed create/update for one export entry. Move and reject are classified
 * elsewhere once identity and diagnostics are in play.
 */
export function classifyCreateOrUpdate(
  workspaceContent: string | undefined,
  exportContent: string
): 'create' | 'update' | 'unchanged' {
  if (workspaceContent === undefined) return 'create'
  if (workspaceContent === exportContent) return 'unchanged'
  return 'update'
}

/**
 * Mirror writeNotebookExport.assertSafeEntryPath, plus empty path segments, as
 * a non-throwing reason so dry-run can still return a string report.
 */
export function unsafePathReason(path: string): string | undefined {
  if (
    path.startsWith('/') ||
    path.includes('\\') ||
    path.split('/').includes('..') ||
    path.split('/').includes('')
  ) {
    return 'unsafe path — not a portable pull target'
  }
  return
}

function basename(path: string): string {
  const slash = path.lastIndexOf('/')
  return slash === -1 ? path : path.slice(slash + 1)
}

function isSyncMetadataPath(path: string): boolean {
  return path.split('/').includes(SYNC_METADATA_SEGMENT)
}

function isReservedBasename(path: string): boolean {
  return RESERVED_BASENAMES.has(basename(path))
}

/** Markdown paths that collide exactly or by case with another zip entry. */
export function duplicateMarkdownPaths(
  zipFileNames: readonly string[]
): Set<string> {
  const seen = new Map<string, string>()
  const duplicates = new Set<string>()
  for (const name of zipFileNames) {
    if (!name.endsWith(MARKDOWN_SUFFIX)) continue
    const key = name.toLowerCase()
    const first = seen.get(key)
    if (first !== undefined) {
      duplicates.add(first)
      duplicates.add(name)
    } else {
      seen.set(key, name)
    }
  }
  return duplicates
}

export type ClassifiedPullNote =
  | {
      readonly action: 'create' | 'update'
      readonly path: string
      readonly workspaceContent: string
      readonly exportContent: string
    }
  | {
      readonly action: 'move'
      readonly path: string
      readonly fromPath: string
      readonly workspaceContent: string
      readonly exportContent: string
    }
  | {
      readonly action: 'reject'
      readonly path: string
      readonly reason: string
    }

function rejectReason(
  path: string,
  duplicates: ReadonlySet<string>
): string | undefined {
  if (duplicates.has(path)) {
    return 'duplicate export path — resolve before pull'
  }
  const unsafe = unsafePathReason(path)
  if (unsafe !== undefined) return unsafe
  if (isSyncMetadataPath(path)) {
    return 'sync metadata under .doughnut-sync — never a pull target'
  }
  if (isReservedBasename(path)) {
    return 'reserved role file — not an ordinary pull target'
  }
  return
}

/**
 * Classify export Markdown notes against the workspace: reject diagnostics
 * first, then identity move when the same doughnut_id sits at different paths,
 * otherwise path-keyed create/update.
 */
export function classifyPreviewPullNotes(
  workspace: ReadonlyMap<string, string>,
  exported: ReadonlyMap<string, string>,
  zipFileNames: readonly string[] = [...exported.keys()]
): ClassifiedPullNote[] {
  const duplicates = duplicateMarkdownPaths(zipFileNames)
  const workspaceById = indexPathsByDoughnutId(workspace)
  const paths = new Set([
    ...[...exported.keys()].filter((path) => path.endsWith(MARKDOWN_SUFFIX)),
    ...[...duplicates].filter((path) => path.endsWith(MARKDOWN_SUFFIX)),
  ])
  const sorted = [...paths].sort((a, b) => (a < b ? -1 : a > b ? 1 : 0))

  const classified: ClassifiedPullNote[] = []
  for (const path of sorted) {
    const reason = rejectReason(path, duplicates)
    if (reason !== undefined) {
      classified.push({ action: 'reject', path, reason })
      continue
    }
    const exportContent = exported.get(path)
    if (exportContent === undefined) continue

    const id = extractDoughnutId(exportContent)
    const fromPath = id === undefined ? undefined : workspaceById.get(id)
    if (fromPath !== undefined && fromPath !== path) {
      classified.push({
        action: 'move',
        path,
        fromPath,
        workspaceContent: workspace.get(fromPath) ?? '',
        exportContent,
      })
      continue
    }
    const action = classifyCreateOrUpdate(workspace.get(path), exportContent)
    if (action === 'unchanged') continue
    classified.push({
      action,
      path,
      workspaceContent: workspace.get(path) ?? '',
      exportContent,
    })
  }
  return classified
}
