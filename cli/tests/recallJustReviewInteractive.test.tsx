import * as fs from 'node:fs'
import { MemoryTrackerController, RecallsController } from 'doughnut-api'
import makeMe from 'doughnut-test-fixtures/makeMe'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import {
  LEAVE_RECALL_PROMPT,
  RECALL_SESSION_STOPPED_LINE,
} from '../src/commands/recall/leaveRecallSessionCopy.js'
import { InteractiveCliApp } from '../src/InteractiveCliApp.js'
import { formatVersionOutput } from '../src/commands/version.js'
import {
  pressEscape,
  pressEscapeAndWaitForCancelledLine,
  renderInkWhenCommandLineReady,
  stripAnsi,
  waitForFrames,
  waitForLastFrame,
} from './inkTestHelpers.js'
import { tempConfigWithToken } from './tempConfigTestHelpers.js'

const baseNoteTimes = {
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

describe('recall just-review (interactive)', () => {
  let configDir: string
  let savedConfigDir: string | undefined
  let recallingSpy: ReturnType<typeof vi.spyOn>
  let showMemoryTrackerSpy: ReturnType<typeof vi.spyOn>
  let getRecallPromptsSpy: ReturnType<typeof vi.spyOn>
  let askAQuestionSpy: ReturnType<typeof vi.spyOn>
  let markAsRecalledSpy: ReturnType<typeof vi.spyOn>

  async function untilPlain(
    frames: string[],
    ok: (plain: string) => boolean
  ): Promise<void> {
    await waitForFrames(
      () => frames.join('\n'),
      (c) => ok(stripAnsi(c))
    )
  }

  async function untilPlainHas(
    frames: string[],
    includes: string[],
    excludes?: string[]
  ): Promise<void> {
    await untilPlain(frames, (p) => {
      if (!includes.every((s) => p.includes(s))) return false
      if (excludes?.some((s) => p.includes(s))) return false
      return true
    })
  }

  async function untilLastFrame(
    frames: string[],
    ok: (plain: string) => boolean
  ): Promise<void> {
    await waitForFrames(
      () => stripAnsi(frames.at(-1) ?? ''),
      (f) => ok(f)
    )
  }

  function startRecall(stdin: { write(data: string): void }) {
    stdin.write('/recall\r')
  }

  async function waitRememberAlpha(
    frames: string[],
    opts?: { ynHint: boolean }
  ) {
    const inc = ['Yes, I remember?', 'Alpha']
    if (opts?.ynHint) inc.push('(y/n)')
    await untilPlainHas(frames, inc)
  }

  async function waitRememberBeta(frames: string[]) {
    await untilPlainHas(frames, ['Yes, I remember?', 'Beta'])
  }

  async function waitLoadMore(frames: string[]) {
    await untilPlainHas(frames, ['Load more from next 3 days?', '(Y/n)'])
  }

  async function backspaceClearsTyped(
    stdin: { write(data: string): void },
    frames: string[],
    rejectedInBuffer: string
  ) {
    stdin.write('\x7f')
    await untilLastFrame(
      frames,
      (f) => f.includes('> ') && !f.includes(rejectedInBuffer)
    )
  }

  async function emptyEnterAndInvalidLineStayOnRemember(
    stdin: { write(data: string): void },
    frames: string[],
    noteTitle: string,
    summaryNotYet: string
  ) {
    await untilPlainHas(
      frames,
      ['Yes, I remember?', noteTitle],
      [summaryNotYet]
    )
    stdin.write('\r')
    await untilPlainHas(
      frames,
      ['Yes, I remember?', noteTitle],
      [summaryNotYet]
    )
    stdin.write('q\r')
    await untilPlainHas(
      frames,
      ['Yes, I remember?', noteTitle],
      [summaryNotYet]
    )
    await backspaceClearsTyped(stdin, frames, '> q')
  }

  function alphaNoteRealm() {
    return makeMe.aNoteRealm
      .title('Alpha')
      .notebookTitle('NB')
      .details('body')
      .createdAt(baseNoteTimes.createdAt)
      .updatedAt(baseNoteTimes.updatedAt)
      .please()
  }

  function mockShowMemoryTrackerCardForRealm(
    noteRealm: ReturnType<typeof alphaNoteRealm>
  ) {
    showMemoryTrackerSpy.mockResolvedValue({
      data: makeMe.aMemoryTracker
        .nextRecallAt('2026-06-01T00:00:00Z')
        .ofNote(noteRealm)
        .please(),
    } as Awaited<ReturnType<typeof MemoryTrackerController.showMemoryTracker>>)
  }

  function mockMarkAsRecalledCounting() {
    const markAsRecalledCount = { n: 0 }
    markAsRecalledSpy.mockImplementation(async () => {
      markAsRecalledCount.n += 1
      return {
        data: makeMe.aMemoryTracker.please(),
      } as Awaited<ReturnType<typeof MemoryTrackerController.markAsRecalled>>
    })
    return markAsRecalledCount
  }

  function setupTwoDueJustReviewItemsMocks() {
    let recallN = 0
    recallingSpy.mockImplementation(async () => {
      recallN += 1
      const trackers =
        recallN === 1
          ? [{ memoryTrackerId: 1, spelling: false }]
          : recallN === 2
            ? [{ memoryTrackerId: 2, spelling: false }]
            : []
      return {
        data: makeMe.aDueMemoryTrackersList
          .totalAssimilatedCount(0)
          .toRepeat(trackers)
          .please(),
      } as Awaited<ReturnType<typeof RecallsController.recalling>>
    })

    showMemoryTrackerSpy.mockImplementation(
      async (opts: { path: { memoryTracker: number } }) => {
        const id = opts.path.memoryTracker
        const title = id === 1 ? 'Alpha' : 'Beta'
        const noteRealm = makeMe.aNoteRealm
          .title(title)
          .notebookTitle('NB')
          .details('body')
          .createdAt(baseNoteTimes.createdAt)
          .updatedAt(baseNoteTimes.updatedAt)
          .please()

        return {
          data: makeMe.aMemoryTracker
            .nextRecallAt('2026-06-01T00:00:00Z')
            .ofNote(noteRealm)
            .please(),
        } as Awaited<
          ReturnType<typeof MemoryTrackerController.showMemoryTracker>
        >
      }
    )
  }

  beforeEach(() => {
    configDir = tempConfigWithToken()
    savedConfigDir = process.env.DOUGHNUT_CONFIG_DIR
    process.env.DOUGHNUT_CONFIG_DIR = configDir

    let recallingCall = 0
    recallingSpy = vi
      .spyOn(RecallsController, 'recalling')
      .mockImplementation(async () => {
        recallingCall += 1
        const trackers =
          recallingCall === 1 ? [{ memoryTrackerId: 1, spelling: false }] : []
        return {
          data: makeMe.aDueMemoryTrackersList
            .totalAssimilatedCount(0)
            .toRepeat(trackers)
            .please(),
        } as Awaited<ReturnType<typeof RecallsController.recalling>>
      })

    showMemoryTrackerSpy = vi.spyOn(
      MemoryTrackerController,
      'showMemoryTracker'
    )

    getRecallPromptsSpy = vi
      .spyOn(MemoryTrackerController, 'getRecallPrompts')
      .mockResolvedValue({
        data: [],
      } as Awaited<ReturnType<typeof MemoryTrackerController.getRecallPrompts>>)

    askAQuestionSpy = vi
      .spyOn(MemoryTrackerController, 'askAQuestion')
      .mockRejectedValue(new Error('stub: no quiz in just-review tests'))

    markAsRecalledSpy = vi
      .spyOn(MemoryTrackerController, 'markAsRecalled')
      .mockResolvedValue({
        data: makeMe.aMemoryTracker.please(),
      } as Awaited<ReturnType<typeof MemoryTrackerController.markAsRecalled>>)
  })

  afterEach(() => {
    recallingSpy.mockRestore()
    showMemoryTrackerSpy.mockRestore()
    getRecallPromptsSpy.mockRestore()
    askAQuestionSpy.mockRestore()
    markAsRecalledSpy.mockRestore()
    if (savedConfigDir === undefined) {
      delete process.env.DOUGHNUT_CONFIG_DIR
    } else {
      process.env.DOUGHNUT_CONFIG_DIR = savedConfigDir
    }
    fs.rmSync(configDir, { recursive: true, force: true })
  })

  test('empty Enter and non-y/n committed line do not recall; y then completes once', async () => {
    const markAsRecalledCount = mockMarkAsRecalledCounting()
    mockShowMemoryTrackerCardForRealm(alphaNoteRealm())

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )
    expect(stripAnsi(frames.join('\n'))).toContain(formatVersionOutput())

    startRecall(stdin)
    await waitRememberAlpha(frames, { ynHint: true })
    await emptyEnterAndInvalidLineStayOnRemember(
      stdin,
      frames,
      'Alpha',
      'Recalled 1 note'
    )

    stdin.write('y\r')
    await waitLoadMore(frames)
    stdin.write('n\r')
    await untilPlain(frames, (p) => p.includes('Recalled 1 note'))
    expect(markAsRecalledCount.n).toBe(1)
  })

  test('load more empty Enter uses default yes → Recalled 1 note', async () => {
    const markAsRecalledCount = mockMarkAsRecalledCounting()
    mockShowMemoryTrackerCardForRealm(alphaNoteRealm())

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitRememberAlpha(frames)
    stdin.write('y\r')
    await waitLoadMore(frames)
    stdin.write('\r')
    await untilPlain(frames, (p) => p.includes('Recalled 1 note'))
    expect(markAsRecalledCount.n).toBe(1)
  })

  test('load more y with empty extended window → Recalled 1 note', async () => {
    const markAsRecalledCount = mockMarkAsRecalledCounting()
    mockShowMemoryTrackerCardForRealm(alphaNoteRealm())

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitRememberAlpha(frames)
    stdin.write('y\r')
    await waitLoadMore(frames)
    stdin.write('y\r')
    await untilPlain(frames, (p) => p.includes('Recalled 1 note'))
    expect(markAsRecalledCount.n).toBe(1)
  })

  test('Esc from remember card shows leave recall confirm without markAsRecalled', async () => {
    mockMarkAsRecalledCounting()
    mockShowMemoryTrackerCardForRealm(alphaNoteRealm())

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitRememberAlpha(frames)
    expect(markAsRecalledSpy).not.toHaveBeenCalled()

    pressEscape(stdin)
    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) => p.includes(LEAVE_RECALL_PROMPT) && p.includes('(y/n)')
    )

    expect(markAsRecalledSpy).not.toHaveBeenCalled()
  })

  test('after Esc on remember card, y settles Recall session stopped without markAsRecalled', async () => {
    mockMarkAsRecalledCounting()
    mockShowMemoryTrackerCardForRealm(alphaNoteRealm())

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitRememberAlpha(frames)
    pressEscape(stdin)
    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) => p.includes(LEAVE_RECALL_PROMPT)
    )

    stdin.write('y\r')
    await untilPlain(frames, (p) => p.includes(RECALL_SESSION_STOPPED_LINE))

    expect(markAsRecalledSpy).not.toHaveBeenCalled()
  })

  test('after Esc on remember card, n returns to Yes, I remember without markAsRecalled', async () => {
    mockMarkAsRecalledCounting()
    mockShowMemoryTrackerCardForRealm(alphaNoteRealm())

    const { stdin, frames, lastFrame } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitRememberAlpha(frames)
    pressEscape(stdin)
    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (p) => p.includes(LEAVE_RECALL_PROMPT)
    )

    stdin.write('n\r')
    await waitForLastFrame(lastFrame, (f) => {
      return (
        f.includes('Yes, I remember?') &&
        f.includes('Alpha') &&
        !f.includes(LEAVE_RECALL_PROMPT)
      )
    })

    expect(markAsRecalledSpy).not.toHaveBeenCalled()
  })

  test('Escape on load more acts like no → Recalled 1 note', async () => {
    const markAsRecalledCount = mockMarkAsRecalledCounting()
    mockShowMemoryTrackerCardForRealm(alphaNoteRealm())

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitRememberAlpha(frames)
    stdin.write('y\r')
    await waitLoadMore(frames)
    stdin.write('\u001b')
    await untilPlain(frames, (p) => p.includes('Recalled 1 note'))
    expect(markAsRecalledCount.n).toBe(1)
  })

  test('load more y with empty extended window after two recalls → Recalled 2 notes', async () => {
    setupTwoDueJustReviewItemsMocks()
    const markAsRecalledCount = mockMarkAsRecalledCounting()

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitRememberAlpha(frames)
    stdin.write('y\r')
    await waitRememberBeta(frames)
    stdin.write('y\r')
    await waitLoadMore(frames)
    stdin.write('y\r')
    await untilPlain(frames, (p) => p.includes('Recalled 2 notes'))
    expect(markAsRecalledCount.n).toBe(2)
  })

  test('load more n does not call recalling with dueindays 3', async () => {
    const dueindaysSeen: number[] = []
    recallingSpy.mockImplementation(
      async (opts: Parameters<typeof RecallsController.recalling>[0]) => {
        dueindaysSeen.push(opts.query.dueindays ?? 0)
        const n = dueindaysSeen.length
        const trackers =
          n === 1 ? [{ memoryTrackerId: 1, spelling: false }] : []
        return {
          data: makeMe.aDueMemoryTrackersList
            .totalAssimilatedCount(0)
            .toRepeat(trackers)
            .please(),
        } as Awaited<ReturnType<typeof RecallsController.recalling>>
      }
    )

    mockShowMemoryTrackerCardForRealm(alphaNoteRealm())

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitRememberAlpha(frames)
    stdin.write('y\r')
    await waitLoadMore(frames)
    stdin.write('n\r')
    await untilPlain(frames, (p) => p.includes('Recalled 1 note'))

    expect(dueindaysSeen).toEqual([0, 0])
  })

  test('missing note title falls back to Note; empty details; no notebook line', async () => {
    mockShowMemoryTrackerCardForRealm(
      makeMe.aNoteRealm
        .title('   ')
        .details('')
        .createdAt(baseNoteTimes.createdAt)
        .updatedAt(baseNoteTimes.updatedAt)
        .please()
    )

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )
    startRecall(stdin)
    await untilPlain(frames, (p) => {
      return (
        p.includes('Yes, I remember?') &&
        p.includes('Note') &&
        !p.includes('Alpha')
      )
    })

    const combined = stripAnsi(frames.join('\n'))
    expect(combined).toContain('Note')
    expect(combined).not.toContain('Alpha')
    stdin.write('n\r')
    await untilPlain(frames, (p) => p.includes('Marked as not recalled.'))
  })

  test('two due just-review items: y twice then n on load more → Recalled 2 notes', async () => {
    setupTwoDueJustReviewItemsMocks()
    const markAsRecalledCount = mockMarkAsRecalledCounting()

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitRememberAlpha(frames)
    stdin.write('y\r')
    await waitRememberBeta(frames)
    stdin.write('y\r')
    await waitLoadMore(frames)
    stdin.write('n\r')
    await untilPlain(frames, (p) => p.includes('Recalled 2 notes'))
    expect(markAsRecalledCount.n).toBe(2)
  })

  test('Escape during initial load shows Cancelled when recalling honors signal', async () => {
    recallingSpy.mockImplementation(
      async (options: { signal?: AbortSignal }) => {
        const { signal } = options
        if (signal === undefined) {
          throw new Error('expected AbortSignal from recall load')
        }
        await new Promise<never>((_, reject) => {
          signal.addEventListener(
            'abort',
            () => {
              reject(new DOMException('Aborted', 'AbortError'))
            },
            { once: true }
          )
        })
      }
    )

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitForFrames(
      () => frames.join('\n'),
      (c) => stripAnsi(c).includes('Loading recall')
    )

    await pressEscapeAndWaitForCancelledLine(stdin, () => frames.join('\n'), {
      normalize: stripAnsi,
    })
    expect(stripAnsi(frames.join('\n'))).toContain('/recall')
  })

  test('Escape during mark as recalled shows Cancelled when mark honors signal', async () => {
    mockShowMemoryTrackerCardForRealm(alphaNoteRealm())
    let markEntered = false
    markAsRecalledSpy.mockImplementation(
      async (options: { signal?: AbortSignal }) => {
        const { signal } = options
        if (signal === undefined) {
          throw new Error('expected AbortSignal from markAsRecalled')
        }
        markEntered = true
        await new Promise<never>((_, reject) => {
          signal.addEventListener(
            'abort',
            () => {
              reject(new DOMException('Aborted', 'AbortError'))
            },
            { once: true }
          )
        })
      }
    )

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitRememberAlpha(frames)

    stdin.write('y\r')
    await waitForFrames(
      () => frames.join('\n'),
      () => markEntered
    )

    await pressEscapeAndWaitForCancelledLine(stdin, () => frames.join('\n'), {
      normalize: stripAnsi,
    })
  })

  test('empty Enter and non-y/n on second item do not recall; y then completes session', async () => {
    setupTwoDueJustReviewItemsMocks()
    const markAsRecalledCount = mockMarkAsRecalledCounting()

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitRememberAlpha(frames)
    stdin.write('y\r')
    await waitRememberBeta(frames)
    await emptyEnterAndInvalidLineStayOnRemember(
      stdin,
      frames,
      'Beta',
      'Recalled 2 notes'
    )

    expect(markAsRecalledCount.n).toBe(1)

    stdin.write('y\r')
    await waitLoadMore(frames)
    stdin.write('n\r')
    await untilPlain(frames, (p) => p.includes('Recalled 2 notes'))
    expect(markAsRecalledCount.n).toBe(2)
  })
})
