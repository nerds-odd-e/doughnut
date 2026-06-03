import { describeRecallJustReviewInteractive } from './recallJustReviewInteractive.suite.js'
import { pendingUntilAbort } from './inkTestHelpers.js'
import type { InkWaitHelpers } from './recallJustReviewInteractive.waits.js'

describeRecallJustReviewInteractive((api) => {
  const {
    test,
    expect,
    InteractiveCliApp,
    RECALL_SESSION_STOPPED_LINE,
    renderInkWhenCommandLineReady,
    pressEscapeAndWaitForCancelledLine,
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
        await pendingUntilAbort(signal)
      }
    )

    const { stdin, lastStrippedFrame, waitForLastFrameToInclude } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(stdin)
    await waitForLastFrameToInclude('Loading recall')

    await pressEscapeAndWaitForCancelledLine(stdin, lastStrippedFrame)
    await waitForLastFrameToInclude('/recall')
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
        await pendingUntilAbort(signal)
      }
    )

    const { stdin, lastStrippedFrame, ...ink } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(stdin)
    await waitRememberCard(ink, 'Alpha')

    stdin.write('y\r')
    await waitForFrames(lastStrippedFrame, () => markEntered)

    await pressEscapeAndWaitForCancelledLine(stdin, lastStrippedFrame)
  })
})
