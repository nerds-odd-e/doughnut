import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'

const BASELINE_RELATIVE_PATH = join('.doughnut-sync', 'baseline.json')

/**
 * `notes` holds, per note path, the content Doughnut and the workspace were
 * last seen to agree on — a merge base, not a snapshot of either side as it
 * stands. A note the two sides have never yet been seen to agree on is absent.
 */
type PushBaselineFile = {
  readonly notebookId: number
  readonly notes: Readonly<Record<string, string>>
}

/**
 * The content each note's two sides were last seen to agree on, as
 * `/push --dry-run` recorded it in this workspace.
 *
 * Empty for a workspace that has never previewed a push, or whose stored
 * baseline belongs to a different notebook — a workspace reused for a
 * different notebook has no useful history to compare against.
 */
export function loadPushBaseline(
  workspacePath: string,
  notebookId: number
): ReadonlyMap<string, string> {
  const path = join(workspacePath, BASELINE_RELATIVE_PATH)
  if (!existsSync(path)) return new Map()

  const parsed = JSON.parse(readFileSync(path, 'utf8')) as PushBaselineFile
  if (parsed.notebookId !== notebookId) return new Map()

  return new Map(Object.entries(parsed.notes))
}

/** Persist the content the two sides now agree on, note by note. */
export function savePushBaseline(
  workspacePath: string,
  notebookId: number,
  notes: ReadonlyMap<string, string>
): void {
  const path = join(workspacePath, BASELINE_RELATIVE_PATH)
  mkdirSync(dirname(path), { recursive: true })
  const file: PushBaselineFile = {
    notebookId,
    notes: Object.fromEntries(notes),
  }
  writeFileSync(path, JSON.stringify(file), 'utf8')
}
