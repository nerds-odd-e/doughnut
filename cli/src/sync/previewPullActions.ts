/**
 * What a dry-run pull would do with one exported Markdown path.
 *
 * Kept separate from push `NoteDiffStatus` (`pull` | `push` | `conflict`) so
 * `/push --dry-run` labeling stays untouched.
 */
export type PreviewPullAction = 'create' | 'update' | 'move' | 'reject'

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
    if (!path.endsWith('.md')) continue
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

/**
 * Classify export Markdown notes against the workspace: identity move when the
 * same doughnut_id sits at different paths; otherwise path-keyed create/update.
 */
export function classifyPreviewPullNotes(
  workspace: ReadonlyMap<string, string>,
  exported: ReadonlyMap<string, string>
): ClassifiedPullNote[] {
  const workspaceById = indexPathsByDoughnutId(workspace)
  const sorted = [...exported]
    .filter(([path]) => path.endsWith('.md'))
    .sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0))

  const classified: ClassifiedPullNote[] = []
  for (const [path, exportContent] of sorted) {
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
