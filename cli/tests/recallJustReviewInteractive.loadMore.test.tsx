import { describeRecallJustReviewInteractive } from './recallJustReviewInteractive.suite.js'

describeRecallJustReviewInteractive((api) => {
  const {
    test,
    expect,
    makeMe,
    RecallsController,
    InteractiveCliApp,
    renderInkWhenCommandLineReady,
    pressEscape,
    waitRememberCard,
    waitLoadMore,
    waitRecalledSummary,
    recallSingleAlphaToLoadMore,
    startRecall,
    alphaNoteRealm,
    mockShowMemoryTrackerCardForRealm,
    mockMarkAsRecalledCounting,
    mockTwoDueThenEmptyExtendedRecalling,
  } = api
  test('load more empty Enter uses default yes → Recalled 1 note', async () => {
    const markAsRecalledCount = mockMarkAsRecalledCounting()
    mockShowMemoryTrackerCardForRealm(alphaNoteRealm())

    const { stdin, ...ink } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    await recallSingleAlphaToLoadMore(stdin, ink)
    stdin.write('\r')
    await waitRecalledSummary(ink, 'Recalled 1 note')
    expect(markAsRecalledCount.n).toBe(1)
  })

  test('load more y shows Loading more… while extended recalling is in flight', async () => {
    const markAsRecalledCount = mockMarkAsRecalledCounting()
    mockShowMemoryTrackerCardForRealm(alphaNoteRealm())

    type RecallingResult = Awaited<
      ReturnType<typeof RecallsController.recalling>
    >
    let resolveExtended: ((value: RecallingResult) => void) | undefined
    const extendedRecallingPromise = new Promise<RecallingResult>((resolve) => {
      resolveExtended = resolve
    })

    let recallingN = 0
    api.recallingSpy.mockImplementation(
      async (opts: Parameters<typeof RecallsController.recalling>[0]) => {
        recallingN += 1
        const due = opts.query.dueindays ?? 0
        if (recallingN === 1 && due === 0) {
          return {
            data: makeMe.aDueMemoryTrackersList
              .totalAssimilatedCount(0)
              .toRepeat([{ memoryTrackerId: 1, spelling: false }])
              .please(),
          } as RecallingResult
        }
        if (due === 3) {
          return extendedRecallingPromise
        }
        return {
          data: makeMe.aDueMemoryTrackersList
            .totalAssimilatedCount(0)
            .toRepeat([])
            .please(),
        } as RecallingResult
      }
    )

    const { stdin, ...ink } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    await recallSingleAlphaToLoadMore(stdin, ink)
    stdin.write('y\r')

    await ink.waitForLastFrameToInclude('Loading more…')

    resolveExtended?.({
      data: makeMe.aDueMemoryTrackersList
        .totalAssimilatedCount(0)
        .toRepeat([])
        .please(),
    })

    await waitRecalledSummary(ink, 'Recalled 1 note')
    expect(markAsRecalledCount.n).toBe(1)
  })

  test('Escape on load more acts like no → Recalled 1 note', async () => {
    const markAsRecalledCount = mockMarkAsRecalledCounting()
    mockShowMemoryTrackerCardForRealm(alphaNoteRealm())

    const { stdin, ...ink } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    await recallSingleAlphaToLoadMore(stdin, ink)
    await pressEscape(stdin)
    await waitRecalledSummary(ink, 'Recalled 1 note')
    expect(markAsRecalledCount.n).toBe(1)
  })

  test('load more y with empty extended window after two recalls → Recalled 2 notes', async () => {
    mockTwoDueThenEmptyExtendedRecalling()
    const markAsRecalledCount = mockMarkAsRecalledCounting()

    const { stdin, ...ink } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitRememberCard(ink, 'Alpha')
    stdin.write('y\r')
    await waitRememberCard(ink, 'Beta')
    stdin.write('y\r')
    await waitLoadMore(ink)
    stdin.write('y\r')
    await waitRecalledSummary(ink, 'Recalled 2 notes')
    expect(markAsRecalledCount.n).toBe(2)
    expect(api.recallingSpy).toHaveBeenCalledTimes(2)
  })

  test('load more n does not call recalling with dueindays 3', async () => {
    const dueindaysSeen: number[] = []
    api.recallingSpy.mockImplementation(
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

    const { stdin, ...ink } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    await recallSingleAlphaToLoadMore(stdin, ink)
    stdin.write('n\r')
    await waitRecalledSummary(ink, 'Recalled 1 note')

    expect(dueindaysSeen).toEqual([0])
  })
})
