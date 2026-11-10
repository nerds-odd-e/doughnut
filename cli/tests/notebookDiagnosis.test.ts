import * as fs from 'node:fs'
import { join } from 'node:path'
import { afterEach, describe, expect, test, vi } from 'vitest'
import { getApiConfig, NotebookController } from 'donut-api'
import makeMe from 'donut-test-fixtures/makeMe'
import { run } from '../src/run.js'
import {
  installNotebookCliRunFixture,
  runGit,
} from './notebookClone.testHelpers.js'
import { initBoundCheckout } from './notebookGit.testHelpers.js'
import {
  commitFileChange,
  localGitObservation,
} from './notebookPublish.testHelpers.js'

describe('notebook diagnosis', () => {
  const ctx = installNotebookCliRunFixture('donut-diagnosis-test-')
  afterEach(() => vi.restoreAllMocks())

  test('reviews exact server differences while retaining pending commits and dirty work', async () => {
    const directory = initBoundCheckout(
      ctx.getWorkDir(),
      getApiConfig().apiBaseUrl
    )
    commitFileChange(directory, 'pending local commit\n', 'pending')
    fs.writeFileSync(join(directory, 'note.md'), 'staged authored work\n')
    runGit(['add', 'note.md'], directory)
    fs.writeFileSync(join(directory, 'note.md'), 'dirty authored work\n')
    fs.writeFileSync(
      join(directory, 'untracked.md'),
      'untracked authored work\n'
    )
    const before = localGitObservation(directory)
    const pointer =
      'version https://git-lfs.github.com/spec/v1\noid sha256:' +
      'a'.repeat(64) +
      '\nsize 3\n'
    const bytes = (text: string) => Buffer.from(text)
    const report = makeMe.aNotebookGitDiagnosis
      .difference(
        'README.md',
        bytes('old README\n'),
        bytes('reviewed README  \n')
      )
      .difference('added.md', null, bytes('new note without final newline'))
      .difference('empty/.keep', null, bytes(''))
      .difference(
        'file.bin',
        bytes(pointer),
        bytes(pointer.replace('size 3', 'size 4'))
      )
      .difference('removed.md', bytes('removed note\n'), null)
      .difference('raw.bin', new Uint8Array([0, 255]), new Uint8Array([0, 254]))
      .please()
    const backend = vi
      .spyOn(NotebookController, 'diagnoseNotebookGit')
      .mockResolvedValue({ data: report } as Awaited<
        ReturnType<typeof NotebookController.diagnoseNotebookGit>
      >)
    const output = vi.spyOn(console, 'log').mockImplementation(() => undefined)

    await run(['notebook', 'diagnose', directory])

    const text = output.mock.calls.map((call) => call[0]).join('\n')
    expect(text).toContain(`Accepted head: ${report.acceptedHead}`)
    expect(text).toContain(`Accepted tree: ${report.acceptedTree}`)
    expect(text).toContain(`Live tree: ${report.liveTree}`)
    expect(text).toContain('changed: README.md')
    expect(text).toContain('-old README\n+reviewed README  \n')
    expect(text).toContain('live-only: added.md')
    expect(text).toContain('\\ No newline at end of file')
    expect(text).toContain('live-only: empty/.keep')
    expect(text).toContain('Accepted bytes: 0; live bytes: 0')
    expect(text).toContain('-size 3\n+size 4')
    expect(text).toContain('accepted-only: removed.md')
    expect(text).toContain('Accepted content (base64): AP8=')
    expect(text).toContain('Live content (base64): AP4=')
    expect(text).toContain('Pull alone cannot repair server projection drift')
    expect(text).toContain('explicitly reviewed forward commit')
    expect(text).toContain(
      `donut notebook recover ${directory} ${report.acceptedHead} ${report.liveTree}`
    )
    expect(localGitObservation(directory)).toEqual(before)
    expect(runGit(['rev-list', '--count', 'main'], directory)).toBe('2')
    expect(backend).toHaveBeenCalledWith(
      expect.objectContaining({ path: { notebook: 42 } })
    )
  })

  test('reports matching canonical trees', async () => {
    const directory = initBoundCheckout(
      ctx.getWorkDir(),
      getApiConfig().apiBaseUrl
    )
    vi.spyOn(NotebookController, 'diagnoseNotebookGit').mockResolvedValue({
      data: makeMe.aNotebookGitDiagnosis.matching().please(),
    } as Awaited<ReturnType<typeof NotebookController.diagnoseNotebookGit>>)
    const output = vi.spyOn(console, 'log').mockImplementation(() => undefined)

    await run(['notebook', 'diagnose', directory])

    expect(output).toHaveBeenCalledWith(
      'Live Portable content matches accepted main.'
    )
  })
})
