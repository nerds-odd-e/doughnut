import { describe, expect, test } from 'vitest'
import { parseSyncArgument } from '../src/sync/syncArgument.js'

describe('parseSyncArgument', () => {
  test('reads the workspace path after the dry run flag', () => {
    expect(parseSyncArgument('--dry-run ./BenNotebook')).toEqual({
      workspacePath: './BenNotebook',
    })
  })

  test('tolerates extra spacing around the flag and the path', () => {
    expect(parseSyncArgument('  --dry-run   ./BenNotebook  ')).toEqual({
      workspacePath: './BenNotebook',
    })
  })

  test('keeps a path that contains spaces', () => {
    expect(parseSyncArgument('--dry-run ./Ben Notebook')).toEqual({
      workspacePath: './Ben Notebook',
    })
  })

  test('rejects a missing argument', () => {
    expect(parseSyncArgument(undefined)).toEqual({
      error: 'Usage: /sync --dry-run <workspace path>',
    })
  })

  test('rejects a dry run with no path', () => {
    expect(parseSyncArgument('--dry-run')).toEqual({
      error: 'Usage: /sync --dry-run <workspace path>',
    })
  })

  test('rejects a path with no dry run flag', () => {
    expect(parseSyncArgument('./BenNotebook')).toEqual({
      error:
        'Only /sync --dry-run is available. Pulling is not implemented yet.',
    })
  })

  test('rejects a flag it does not know', () => {
    expect(parseSyncArgument('--force ./BenNotebook')).toEqual({
      error:
        'Only /sync --dry-run is available. Pulling is not implemented yet.',
    })
  })
})
