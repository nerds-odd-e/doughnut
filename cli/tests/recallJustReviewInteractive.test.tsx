import * as fs from 'node:fs'
import { MemoryTrackerController, RecallsController } from 'doughnut-api'
import makeMe from 'doughnut-test-fixtures/makeMe'
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import { InteractiveCliApp } from '../src/InteractiveCliApp.js'
import { formatVersionOutput } from '../src/commands/version.js'
import {
  renderInkWhenCommandLineReady,
  stripAnsi,
  waitForFrames,
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
  let markAsRecalledSpy: ReturnType<typeof vi.spyOn>

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
    markAsRecalledSpy.mockRestore()
    if (savedConfigDir === undefined) {
      delete process.env.DOUGHNUT_CONFIG_DIR
    } else {
      process.env.DOUGHNUT_CONFIG_DIR = savedConfigDir
    }
    fs.rmSync(configDir, { recursive: true, force: true })
  })

  test('empty Enter and non-y/n committed line do not recall; y then completes once', async () => {
    const markAsRecalledCount = { n: 0 }
    markAsRecalledSpy.mockImplementation(async () => {
      markAsRecalledCount.n += 1
      return {
        data: makeMe.aMemoryTracker.please(),
      } as Awaited<ReturnType<typeof MemoryTrackerController.markAsRecalled>>
    })

    const noteRealm = makeMe.aNoteRealm
      .title('Alpha')
      .notebookTitle('NB')
      .details('body')
      .createdAt(baseNoteTimes.createdAt)
      .updatedAt(baseNoteTimes.updatedAt)
      .please()

    showMemoryTrackerSpy.mockResolvedValue({
      data: makeMe.aMemoryTracker
        .nextRecallAt('2026-06-01T00:00:00Z')
        .ofNote(noteRealm)
        .please(),
    } as Awaited<ReturnType<typeof MemoryTrackerController.showMemoryTracker>>)

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )
    expect(stripAnsi(frames.join('\n'))).toContain(formatVersionOutput())

    stdin.write('/recall\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        stripAnsi(c).includes('Yes, I remember?') &&
        stripAnsi(c).includes('Alpha')
    )

    stdin.write('\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        stripAnsi(c).includes('Yes, I remember?') &&
        !stripAnsi(c).includes('Recalled successfully')
    )

    stdin.write('q\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        stripAnsi(c).includes('Yes, I remember?') &&
        !stripAnsi(c).includes('Recalled successfully')
    )

    stdin.write('\x7f')
    await waitForFrames(
      () => stripAnsi(frames.at(-1) ?? ''),
      (f) => f.includes('> ') && !f.includes('> q')
    )

    stdin.write('y\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) => stripAnsi(c).includes('Recalled successfully')
    )
    expect(markAsRecalledCount.n).toBe(1)
  })

  test('missing note title falls back to Note; empty details; no notebook line', async () => {
    const noteRealm = makeMe.aNoteRealm
      .title('   ')
      .details('')
      .createdAt(baseNoteTimes.createdAt)
      .updatedAt(baseNoteTimes.updatedAt)
      .please()

    showMemoryTrackerSpy.mockResolvedValue({
      data: makeMe.aMemoryTracker
        .nextRecallAt('2026-06-01T00:00:00Z')
        .ofNote(noteRealm)
        .please(),
    } as Awaited<ReturnType<typeof MemoryTrackerController.showMemoryTracker>>)

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )
    stdin.write('/recall\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) => {
        const plain = stripAnsi(c)
        return (
          plain.includes('Yes, I remember?') &&
          plain.includes('Note') &&
          !plain.includes('Alpha')
        )
      }
    )

    const combined = stripAnsi(frames.join('\n'))
    expect(combined).toContain('Note')
    expect(combined).not.toContain('Alpha')
    stdin.write('n\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) => stripAnsi(c).includes('Marked as not recalled.')
    )
  })

  test('two due just-review items: y twice then recalled successfully', async () => {
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

    const markAsRecalledCount = { n: 0 }
    markAsRecalledSpy.mockImplementation(async () => {
      markAsRecalledCount.n += 1
      return {
        data: makeMe.aMemoryTracker.please(),
      } as Awaited<ReturnType<typeof MemoryTrackerController.markAsRecalled>>
    })

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/recall\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        stripAnsi(c).includes('Yes, I remember?') &&
        stripAnsi(c).includes('Alpha')
    )

    stdin.write('y\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        stripAnsi(c).includes('Yes, I remember?') &&
        stripAnsi(c).includes('Beta')
    )

    stdin.write('y\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) => stripAnsi(c).includes('Recalled successfully')
    )
    expect(markAsRecalledCount.n).toBe(2)
  })

  test('empty Enter and non-y/n on second item do not recall; y then completes session', async () => {
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

    const markAsRecalledCount = { n: 0 }
    markAsRecalledSpy.mockImplementation(async () => {
      markAsRecalledCount.n += 1
      return {
        data: makeMe.aMemoryTracker.please(),
      } as Awaited<ReturnType<typeof MemoryTrackerController.markAsRecalled>>
    })

    const { stdin, frames } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    stdin.write('/recall\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        stripAnsi(c).includes('Yes, I remember?') &&
        stripAnsi(c).includes('Alpha')
    )

    stdin.write('y\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        stripAnsi(c).includes('Yes, I remember?') &&
        stripAnsi(c).includes('Beta')
    )

    stdin.write('\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        stripAnsi(c).includes('Yes, I remember?') &&
        stripAnsi(c).includes('Beta') &&
        !stripAnsi(c).includes('Recalled successfully')
    )

    stdin.write('q\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) =>
        stripAnsi(c).includes('Yes, I remember?') &&
        stripAnsi(c).includes('Beta') &&
        !stripAnsi(c).includes('Recalled successfully')
    )

    stdin.write('\x7f')
    await waitForFrames(
      () => stripAnsi(frames.at(-1) ?? ''),
      (f) => f.includes('> ') && !f.includes('> q')
    )

    expect(markAsRecalledCount.n).toBe(1)

    stdin.write('y\r')
    await waitForFrames(
      () => frames.join('\n'),
      (c) => stripAnsi(c).includes('Recalled successfully')
    )
    expect(markAsRecalledCount.n).toBe(2)
  })
})
