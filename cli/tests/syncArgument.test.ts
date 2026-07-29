import { describe, expect, test } from 'vitest'
import { parseSyncArgument } from '../src/sync/syncArgument.js'

describe('parseSyncArgument', () => {
  test('reads the workspace path after the dry run flag', () => {
    expect(parseSyncArgument('--dry-run ./BenNotebook')).toEqual({
      workspacePath: './BenNotebook',
      dryRun: true,
    })
  })

  test('tolerates extra spacing around the flag and the path', () => {
    expect(parseSyncArgument('  --dry-run   ./BenNotebook  ')).toEqual({
      workspacePath: './BenNotebook',
      dryRun: true,
    })
  })

  test('keeps a path that contains spaces after the dry run flag', () => {
    expect(parseSyncArgument('--dry-run ./Ben Notebook')).toEqual({
      workspacePath: './Ben Notebook',
      dryRun: true,
    })
  })

  test('reads a workspace path without the dry run flag as pull', () => {
    expect(parseSyncArgument('./BenNotebook')).toEqual({
      workspacePath: './BenNotebook',
      dryRun: false,
    })
  })

  test('rejects a missing argument', () => {
    expect(parseSyncArgument(undefined)).toEqual({
      error: 'Usage: /sync [--dry-run] <workspace path>',
    })
  })

  test('rejects a dry run with no path', () => {
    expect(parseSyncArgument('--dry-run')).toEqual({
      error: 'Usage: /sync [--dry-run] <workspace path>',
    })
  })

  test('rejects an unknown flag', () => {
    expect(parseSyncArgument('--force ./BenNotebook')).toEqual({
      error: 'Usage: /sync [--dry-run] <workspace path>',
    })
  })

  test('rejects a longer flag that starts like dry run', () => {
    expect(parseSyncArgument('--dry-run-extra ./BenNotebook')).toEqual({
      error: 'Usage: /sync [--dry-run] <workspace path>',
    })
  })

  test('strips surrounding double quotes from the workspace path', () => {
    expect(parseSyncArgument('"/mnt/d/LeSS/Perf Notebook"')).toEqual({
      workspacePath: '/mnt/d/LeSS/Perf Notebook',
      dryRun: false,
    })
  })
})
