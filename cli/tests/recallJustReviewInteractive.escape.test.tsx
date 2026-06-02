import { describeRecallJustReviewInteractive } from './recallJustReviewInteractive.suite.js'

describeRecallJustReviewInteractive((api) => {
  const {
    test,
    expect,
    InteractiveCliApp,
    RECALL_SESSION_STOPPED_LINE,
    leaveRecallWithYnRe,
    renderInkWhenCommandLineReady,
    pressEscape,
    pressEscapeAndWaitForCancelledLine,
    stripAnsi,
    waitForFrames,
    waitRememberCard,
    startRecall,
    alphaNoteRealm,
    mockShowMemoryTrackerCardForRealm,
    mockMarkAsRecalledCounting,
    waitReturnsToSingleRememberCard,
  } = api
  test('after Esc on remember card, y settles Recall session stopped without markAsRecalled', async () => {
    mockMarkAsRecalledCounting()
    mockShowMemoryTrackerCardForRealm(alphaNoteRealm())

    const { stdin, waitForFramesToInclude, ...ink } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(stdin)
    await waitRememberCard(ink, 'Alpha')
    await pressEscape(stdin)
    await waitForFramesToInclude(leaveRecallWithYnRe)
    expect(api.markAsRecalledSpy).not.toHaveBeenCalled()

    stdin.write('y\r')
    await ink.waitForLastFrameToInclude(RECALL_SESSION_STOPPED_LINE)

    expect(api.markAsRecalledSpy).not.toHaveBeenCalled()
  })

  test('after Esc on remember card, n returns to Yes, I remember without markAsRecalled', async () => {
    mockMarkAsRecalledCounting()
    mockShowMemoryTrackerCardForRealm(alphaNoteRealm())

    const { stdin, waitForFramesToInclude, ...ink } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(stdin)
    await waitRememberCard(ink, 'Alpha')
    await pressEscape(stdin)
    await waitForFramesToInclude(/Leave recall\?/)

    stdin.write('n\r')
    await waitReturnsToSingleRememberCard(ink, 'Alpha')

    expect(api.markAsRecalledSpy).not.toHaveBeenCalled()
  })

  test('empty Enter on leave recall confirm stays on confirm; n returns to remember card', async () => {
    mockMarkAsRecalledCounting()
    mockShowMemoryTrackerCardForRealm(alphaNoteRealm())

    const { stdin, waitForFramesToInclude, ...ink } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(stdin)
    await waitRememberCard(ink, 'Alpha')
    await pressEscape(stdin)
    await waitForFramesToInclude(/Leave recall\?/)

    stdin.write('\r')
    await waitForFramesToInclude(/Leave recall\?/)

    expect(api.markAsRecalledSpy).not.toHaveBeenCalled()

    stdin.write('n\r')
    await waitReturnsToSingleRememberCard(ink, 'Alpha')

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
