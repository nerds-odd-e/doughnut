import { homedir } from 'node:os'
import { join, resolve } from 'node:path'
import { describe, expect, test } from 'vitest'
import { lintWorkspace } from '../src/lint/lintWorkspace.js'
import { useLintWorkspaceFixture } from './lintWorkspaceFixture.js'

describe('lintWorkspace path argument', () => {
  const { workspaceRoot, write, concept, writeRootIndex } =
    useLintWorkspaceFixture()

  test('says so when nothing is there, rather than throwing', () => {
    const missing = join(workspaceRoot(), 'nowhere')

    expect(lintWorkspace(missing)).toBe(`No directory at ${missing}.`)
  })

  test('says so when the path is a file', () => {
    write('apple.md', concept('type: concept', 'apple'))
    const file = join(workspaceRoot(), 'apple.md')

    expect(lintWorkspace(file)).toBe(`No directory at ${file}.`)
  })

  test('strips surrounding quotes, as a shell would', () => {
    write('apple.md', concept('type: concept', 'apple'))
    writeRootIndex()

    expect(lintWorkspace(`"${workspaceRoot()}"`)).toBe(
      'Workspace follows the OKF format.'
    )
  })

  test('expands `~` to the home directory', () => {
    expect(lintWorkspace('~/nowhere-in-home')).toBe(
      `No directory at ${join(homedir(), 'nowhere-in-home')}.`
    )
  })

  test('will not guess at another user from `~`', () => {
    expect(lintWorkspace('~someone/bundle')).toBe(
      "Cannot expand ~someone/bundle: only the current user's home directory (~) is supported."
    )
  })

  test('resolves a relative path against the working directory', () => {
    expect(lintWorkspace('nowhere-relative')).toBe(
      `No directory at ${resolve(process.cwd(), 'nowhere-relative')}.`
    )
  })
})
