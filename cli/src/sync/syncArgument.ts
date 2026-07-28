const DRY_RUN_FLAG = '--dry-run'

const USAGE = 'Usage: /sync --dry-run <workspace path>'

const DRY_RUN_ONLY =
  'Only /sync --dry-run is available. Pulling is not implemented yet.'

export type SyncArgument =
  | { readonly workspacePath: string; readonly error?: undefined }
  | { readonly error: string; readonly workspacePath?: undefined }

/**
 * Read `--dry-run <workspace path>`. Previewing is all this command does for
 * now, so anything else is turned away rather than assumed to mean a pull.
 */
export function parseSyncArgument(argument: string | undefined): SyncArgument {
  const trimmed = (argument ?? '').trim()
  if (trimmed === '') return { error: USAGE }
  if (!trimmed.startsWith(DRY_RUN_FLAG)) return { error: DRY_RUN_ONLY }

  const rest = trimmed.slice(DRY_RUN_FLAG.length)
  // Without separating whitespace this is a longer flag, not the dry run one.
  if (rest !== '' && !/^\s/.test(rest)) return { error: DRY_RUN_ONLY }

  const workspacePath = rest.trim()
  if (workspacePath === '') return { error: USAGE }
  return { workspacePath }
}
