import { describe, expect, test } from 'vitest'
import { parsePushArgument } from '../src/sync/pushArgument.js'

describe('parsePushArgument', () => {
  test('reads the workspace path after the dry run flag', () => {
    expect(parsePushArgument('--dry-run ./BenNotebook')).toEqual({
      workspacePath: './BenNotebook',
    })
  })

  test('tolerates extra spacing around the flag and the path', () => {
    expect(parsePushArgument('  --dry-run   ./BenNotebook  ')).toEqual({
      workspacePath: './BenNotebook',
    })
  })

  test('keeps a path that contains spaces after the dry run flag', () => {
    expect(parsePushArgument('--dry-run ./Ben Notebook')).toEqual({
      workspacePath: './Ben Notebook',
    })
  })

  test('rejects a missing argument', () => {
    expect(parsePushArgument(undefined)).toEqual({
      error: 'Usage: /push --dry-run <workspace path>',
    })
  })

  test('rejects a workspace path with no dry run flag', () => {
    expect(parsePushArgument('./BenNotebook')).toEqual({
      error: 'Usage: /push --dry-run <workspace path>',
    })
  })

  test('rejects a dry run flag with no path', () => {
    expect(parsePushArgument('--dry-run')).toEqual({
      error: 'Usage: /push --dry-run <workspace path>',
    })
  })

  test('rejects an unknown flag', () => {
    expect(parsePushArgument('--force ./BenNotebook')).toEqual({
      error: 'Usage: /push --dry-run <workspace path>',
    })
  })

  test('rejects a longer flag that starts like dry run', () => {
    expect(parsePushArgument('--dry-run-extra ./BenNotebook')).toEqual({
      error: 'Usage: /push --dry-run <workspace path>',
    })
  })

  test('rejects the dry run flag typed after the workspace path', () => {
    expect(parsePushArgument('./BenNotebook --dry-run')).toEqual({
      error: 'Usage: /push --dry-run <workspace path>',
    })
  })

  test('strips surrounding double quotes from the workspace path', () => {
    expect(parsePushArgument('--dry-run "/mnt/d/LeSS/Perf Notebook"')).toEqual({
      workspacePath: '/mnt/d/LeSS/Perf Notebook',
    })
  })
})
