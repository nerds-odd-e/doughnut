import { describeRecallJustReviewInteractive } from './recallJustReviewInteractive.suite.js'
import type { InkWaitHelpers } from './recallJustReviewInteractive.waits.js'

describeRecallJustReviewInteractive((api) => {
  const {
    test,
    expect,
    InteractiveCliApp,
    RECALL_SESSION_STOPPED_LINE,
    renderInkWhenCommandLineReady,
    pressEscapeAndWaitForCancelledLine,
    stripAnsi,
    waitForFrames,
    waitRememberCard,
    startRecall,
    alphaNoteRealm,
    mockShowMemoryTrackerCardForRealm,
    mockMarkAsRecalledCounting,
    reachLeaveRecallOnRemember,
    waitReturnsToSingleRememberCard,
  } = api

  async function inkAtLeaveRecallConfirm() {
    mockMarkAsRecalledCounting()
    mockShowMemoryTrackerCardForRealm(alphaNoteRealm())
    const rendered = await renderInkWhenCommandLineReady(<InteractiveCliApp />)
    startRecall(rendered.stdin)
    await reachLeaveRecallOnRemember(rendered.stdin, rendered, 'Alpha')
    expect(api.markAsRecalledSpy).not.toHaveBeenCalled()
    return rendered
  }

  test.each([
    {
      name: 'after Esc on remember card, y settles Recall session stopped without markAsRecalled',
      run: async (
        stdin: { write: (s: string) => void },
        ink: InkWaitHelpers
      ) => {
        stdin.write('y\r')
        await ink.waitForLastFrameToInclude(RECALL_SESSION_STOPPED_LINE)
      },
    },
    {
      name: 'after Esc on remember card, n returns to Yes, I remember without markAsRecalled',
      run: async (
        _stdin: { write: (s: string) => void },
        ink: InkWaitHelpers
      ) => {
        _stdin.write('n\r')
        await waitReturnsToSingleRememberCard(ink, 'Alpha')
      },
    },
    {
      name: 'empty Enter on leave recall confirm stays on confirm; n returns to remember card',
      run: async (
        stdin: { write: (s: string) => void },
        ink: InkWaitHelpers
      ) => {
        stdin.write('\r')
        await ink.waitForLastFrameToInclude(/Leave recall\?/)
        stdin.write('n\r')
        await waitReturnsToSingleRememberCard(ink, 'Alpha')
      },
    },
  ])('$name', async ({ run }) => {
    const { stdin, ...ink } = await inkAtLeaveRecallConfirm()
    await run(stdin, ink)
    expect(api.markAsRecalledSpy).not.toHaveBeenCalled()
  })

  test('Escape during initial load shows Cancelled when recalling honors signal', async () => {
    api.recallingSpy.mockImplementation(
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

    const { stdin, frames, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(stdin)
    await waitForFramesToInclude('Loading recall')

    await pressEscapeAndWaitForCancelledLine(stdin, () => frames.join('\n'), {
      normalize: stripAnsi,
    })
    await waitForFramesToInclude('/recall')
  })

  test('Escape during mark as recalled shows Cancelled when mark honors signal', async () => {
    mockShowMemoryTrackerCardForRealm(alphaNoteRealm())
    let markEntered = false
    api.markAsRecalledSpy.mockImplementation(
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

    const { stdin, frames, ...ink } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitRememberCard(ink, 'Alpha')

    stdin.write('y\r')
    await waitForFrames(
      () => ink.lastStrippedFrame(),
      () => markEntered
    )

    await pressEscapeAndWaitForCancelledLine(stdin, () => frames.join('\n'), {
      normalize: stripAnsi,
    })
  })
})
