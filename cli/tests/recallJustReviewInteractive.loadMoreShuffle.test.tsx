import { describeRecallJustReviewInteractive } from './recallJustReviewInteractive.suite.js'

describeRecallJustReviewInteractive((api) => {
  const {
    test,
    expect,
    vi,
    makeMe,
    MemoryTrackerController,
    RecallsController,
    InteractiveCliApp,
    renderInkWhenCommandLineReady,
    waitRememberCard,
    recallSingleAlphaToLoadMore,
    mockMarkAsRecalledCounting,
    baseNoteTimes,
  } = api

  test('load more y shows first card in shuffled order for extended window', async () => {
    const randomSpy = vi.spyOn(Math, 'random').mockReturnValue(0)
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
            : id === 11
              ? 'FIRST_AFTER_SHUFFLE'
              : `unexpected-${String(id)}`
        const noteRealm = makeMe.aNoteRealm
          .title(title)
          .content('body')
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

    const { stdin, ...ink } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    try {
      await recallSingleAlphaToLoadMore(stdin, ink)
      stdin.write('y\r')
      await waitRememberCard(ink, 'FIRST_AFTER_SHUFFLE')
      expect(api.recallingSpy).toHaveBeenCalledTimes(2)
    } finally {
      randomSpy.mockRestore()
    }
  })
})
