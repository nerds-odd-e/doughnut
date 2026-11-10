import * as fs from 'node:fs'
import { join } from 'node:path'
import { afterEach, describe, expect, test, vi } from 'vitest'
import { getApiConfig, NotebookController } from 'donut-api'
import { run } from '../src/run.js'
import {
  installNotebookCliRunFixture,
  ProcessExitForTest,
  runGit,
} from './notebookClone.testHelpers.js'
import { initBoundCheckout } from './notebookGit.testHelpers.js'
import {
  commitFileChange,
  localGitObservation,
} from './notebookPublish.testHelpers.js'

const acceptedHead = 'a'.repeat(40)
const liveTree = 'b'.repeat(40)
const recoveredHead = 'c'.repeat(40)

describe('reviewed notebook recovery', () => {
  const ctx = installNotebookCliRunFixture('donut-recovery-test-')
  afterEach(() => vi.restoreAllMocks())

  function checkoutWithPendingAndDirtyWork() {
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
    return directory
  }

  test('submits the explicit review and reports continuation while retaining all local work', async () => {
    const directory = checkoutWithPendingAndDirtyWork()
    const before = localGitObservation(directory)
    const backend = vi
      .spyOn(NotebookController, 'recoverNotebookGit')
      .mockResolvedValue({ data: recoveredHead } as Awaited<
        ReturnType<typeof NotebookController.recoverNotebookGit>
      >)
    const output = vi.spyOn(console, 'log').mockImplementation(() => undefined)

    await run(['notebook', 'recover', directory, acceptedHead, liveTree])

    expect(backend).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { notebook: 42 },
        body: { acceptedHead, liveTree },
        throwOnError: true,
      })
    )
    const text = output.mock.calls.map((call) => call[0]).join('\n')
    expect(text).toContain(
      `Recovered notebook. Accepted head: ${recoveredHead}.`
    )
    expect(text).toContain(`donut notebook pull ${directory}`)
    expect(text).toContain('review and resolve any rebase conflicts')
    expect(text).toContain(`donut notebook publish ${directory}`)
    expect(localGitObservation(directory)).toEqual(before)
    expect(runGit(['rev-list', '--count', 'main'], directory)).toBe('2')
  })

  test('retains all local work and shows the server fresh-review advice on refusal', async () => {
    const directory = checkoutWithPendingAndDirtyWork()
    const before = localGitObservation(directory)
    const message =
      'The notebook changed since this review. Run "donut notebook diagnose" and review the current accepted and live state before recovery.'
    vi.spyOn(NotebookController, 'recoverNotebookGit').mockRejectedValue({
      status: 409,
      errorType: 'RESOURCE_CONFLICT',
      message,
    })

    await expect(
      run(['notebook', 'recover', directory, acceptedHead, liveTree])
    ).rejects.toBeInstanceOf(ProcessExitForTest)

    expect(ctx.getErrorSpy()).toHaveBeenCalledWith(`donut: ${message}`)
    expect(localGitObservation(directory)).toEqual(before)
  })

  test.each([{ review: [] as string[] }, { review: [acceptedHead] }])(
    'requires both explicit review values: $review',
    async ({ review }) => {
      const directory = initBoundCheckout(
        ctx.getWorkDir(),
        getApiConfig().apiBaseUrl
      )
      const backend = vi.spyOn(NotebookController, 'recoverNotebookGit')

      await expect(
        run(['notebook', 'recover', directory, ...review])
      ).rejects.toBeInstanceOf(ProcessExitForTest)

      expect(ctx.getErrorSpy()).toHaveBeenCalledWith(
        'donut: usage: donut notebook recover <directory> <accepted-head> <live-tree>'
      )
      expect(backend).not.toHaveBeenCalled()
    }
  )
})
