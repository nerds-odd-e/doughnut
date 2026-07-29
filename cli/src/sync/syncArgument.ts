const DRY_RUN_FLAG = '--dry-run'

const USAGE = 'Usage: /sync [--dry-run] <workspace path>'

export type SyncArgument =
  | {
      readonly workspacePath: string
      readonly dryRun: boolean
      readonly error?: undefined
    }
  | { readonly error: string; readonly workspacePath?: undefined }

/**
 * Read `[--dry-run] <workspace path>`. Without the flag, the command pulls
 * remote changes into existing workspace files.
 */
export function parseSyncArgument(argument: string | undefined): SyncArgument {
  const trimmed = (argument ?? '').trim()
  if (trimmed === '') return { error: USAGE }

  let dryRun = false
  let workspacePart = trimmed

  if (trimmed.startsWith(DRY_RUN_FLAG)) {
    const afterFlag = trimmed.slice(DRY_RUN_FLAG.length)
    if (afterFlag !== '' && !/^\s/.test(afterFlag)) {
      return { error: USAGE }
    }
    dryRun = true
    workspacePart = afterFlag.trim()
  } else if (trimmed.startsWith('--')) {
    return { error: USAGE }
  }

  if (workspacePart === '') return { error: USAGE }
  // The flag is only read where the usage puts it. Typed after the path it would
  // otherwise become part of the path and be reported as a missing directory,
  // which says nothing about the flag being in the wrong place.
  if (workspacePart.split(/\s+/).includes(DRY_RUN_FLAG)) {
    return { error: USAGE }
  }
  return { workspacePath: stripSurroundingQuotes(workspacePart), dryRun }
}

/** Shell-style quotes are not parsed by the CLI; strip them if the user typed them. */
function stripSurroundingQuotes(path: string): string {
  if (path.length >= 2) {
    const first = path[0]
    const last = path[path.length - 1]
    if ((first === '"' && last === '"') || (first === "'" && last === "'")) {
      return path.slice(1, -1)
    }
  }
  return path
}
