import { stripSurroundingQuotes } from './stripSurroundingQuotes.js'

const DRY_RUN_FLAG = '--dry-run'

const USAGE = 'Usage: /push --dry-run <workspace path>'

export type PushArgument =
  | { readonly workspacePath: string; readonly error?: undefined }
  | { readonly error: string; readonly workspacePath?: undefined }

/**
 * Read `--dry-run <workspace path>`. `/push` is dry-run-only: the flag is
 * mandatory, unlike `/sync`. Any call without it is a usage error.
 */
export function parsePushArgument(argument: string | undefined): PushArgument {
  const trimmed = (argument ?? '').trim()
  if (trimmed === '') return { error: USAGE }

  if (!trimmed.startsWith(DRY_RUN_FLAG)) return { error: USAGE }

  const afterFlag = trimmed.slice(DRY_RUN_FLAG.length)
  if (afterFlag !== '' && !/^\s/.test(afterFlag)) return { error: USAGE }

  const workspacePart = afterFlag.trim()
  if (workspacePart === '') return { error: USAGE }
  // The flag is only read where the usage puts it. Typed after the path it would
  // otherwise become part of the path and be reported as a missing directory,
  // which says nothing about the flag being in the wrong place.
  if (workspacePart.split(/\s+/).includes(DRY_RUN_FLAG)) return { error: USAGE }

  return { workspacePath: stripSurroundingQuotes(workspacePart) }
}
