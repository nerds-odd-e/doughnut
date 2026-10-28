import { describeRecallJustReviewInteractive } from './recallJustReviewInteractive.suite.js'

describeRecallJustReviewInteractive((api) => {
  const {
    test,
    expect,
    makeMe,
    MemoryTrackerController,
    RecallsController,
    InteractiveCliApp,
    renderInkWhenCommandLineReady,
    waitJustReviewCard,
    recallSingleAlphaToLoadMore,
    mockMarkAsRecalledCounting,
  } = api

  test('load more y shows first card in shared due-list order for extended window', async () => {
    let recallingCall = 0
    api.recallingSpy.mockImplementation(
      async (opts: Parameters<typeof RecallsController.recalling>[0]) => {
        recallingCall += 1
        const due = opts.query.dueindays ?? 0
        if (recallingCall === 1) {
          expect(due).toBe(0)
          return {
            data: makeMe.aDueMemoryTrackersList
              .totalAssimilatedCount(0)
              .toRepeat([{ memoryTrackerId: 1, spelling: false }])
              .please(),
          } as Awaited<ReturnType<typeof RecallsController.recalling>>
        }
        expect(due).toBe(3)
        return {
          data: makeMe.aDueMemoryTrackersList
            .totalAssimilatedCount(0)
            .toRepeat([
              { memoryTrackerId: 10, spelling: false },
              { memoryTrackerId: 11, spelling: false },
              { memoryTrackerId: 12, spelling: false },
            ])
            .please(),
        } as Awaited<ReturnType<typeof RecallsController.recalling>>
      }
    )

    mockMarkAsRecalledCounting()
    api.showMemoryTrackerSpy.mockImplementation(
      async (opts: { path: { memoryTracker: number } }) => {
        const id = opts.path.memoryTracker
        const title =
          id === 1
            ? 'Alpha'
            : id === 10
              ? 'FIRST_IN_SHARED_ORDER'
              : `unexpected-${String(id)}`
        const noteRealm = makeMe.aNoteRealm
          .title(title)
          .content('body')
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

    const { stdin, ...ink } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    await recallSingleAlphaToLoadMore(stdin, ink)
    stdin.write('y\r')
    await waitJustReviewCard(ink, 'FIRST_IN_SHARED_ORDER')
    expect(api.recallingSpy).toHaveBeenCalledTimes(2)
  })
})
