/**
 * What a dry-run pull would do with one exported Markdown path.
 *
 * Kept separate from push `NoteDiffStatus` (`pull` | `push` | `conflict`) so
 * `/push --dry-run` labeling stays untouched.
 */
export type PreviewPullAction = 'create' | 'update' | 'move' | 'reject'

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
