import { describeRecallJustReviewInteractive } from './recallJustReviewInteractive.suite.js'

describeRecallJustReviewInteractive((api) => {
  const {
    test,
    expect,
    makeMe,
    MemoryTrackerController,
    RecallsController,
    InteractiveCliApp,
    RECALL_LOADING_NEXT_QUESTION_LABEL,
    renderInkWhenCommandLineReady,
    waitForLastFrame,
    waitRememberCard,
    waitLoadMore,
    waitRecalledSummary,
    emptyEnterAndInvalidLineStayOnRemember,
    startRecall,
    alphaNoteRealm,
    childNoteUnderEnglish,
    mockShowMemoryTrackerCardForRealm,
    mockMarkAsRecalledCounting,
    setupTwoDueJustReviewItemsMocks,
    baseNoteTimes,
  } = api
  test('shows busy label in bordered input while markAsRecalled is pending', async () => {
    mockShowMemoryTrackerCardForRealm(alphaNoteRealm())
    let resolveMark!: (
      value: Awaited<ReturnType<typeof MemoryTrackerController.markAsRecalled>>
    ) => void
    const markPromise = new Promise<
      Awaited<ReturnType<typeof MemoryTrackerController.markAsRecalled>>
    >((resolve) => {
      resolveMark = resolve
    })
    api.markAsRecalledSpy.mockImplementation(() => markPromise)

    const { stdin, ...ink } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitRememberCard(ink, 'Alpha', { ynHint: true })
    stdin.write('y\r')

    await ink.waitForLastFrameToInclude(/Recording review…/)

    resolveMark({
      data: makeMe.aMemoryTracker.please(),
    } as Awaited<ReturnType<typeof MemoryTrackerController.markAsRecalled>>)

    await waitLoadMore(ink)
    stdin.write('n\r')
    await waitRecalledSummary(ink, 'Recalled 1 note')
  })

  test('after y on first just-review, shows loading next until second tracker loads', async () => {
    mockMarkAsRecalledCounting()

    api.recallingSpy.mockResolvedValue({
      data: makeMe.aDueMemoryTrackersList
        .totalAssimilatedCount(0)
        .toRepeat([
          { memoryTrackerId: 1, spelling: false },
          { memoryTrackerId: 2, spelling: false },
        ])
        .please(),
    } as Awaited<ReturnType<typeof RecallsController.recalling>>)

    const noteRealmAlpha = alphaNoteRealm()
    const noteRealmBeta = makeMe.aNoteRealm
      .title('Beta')
      .content('body-beta')
      .createdAt(baseNoteTimes.createdAt)
      .updatedAt(baseNoteTimes.updatedAt)
      .please()

    let resolveMt2!: (
      value: Awaited<
        ReturnType<typeof MemoryTrackerController.showMemoryTracker>
      >
    ) => void
    const mt2Promise = new Promise<
      Awaited<ReturnType<typeof MemoryTrackerController.showMemoryTracker>>
    >((resolve) => {
      resolveMt2 = resolve
    })

    api.showMemoryTrackerSpy.mockImplementation(async (opts) => {
      const id = opts.path.memoryTracker
      if (id === 1) {
        return {
          data: makeMe.aMemoryTracker
            .nextRecallAt('2026-06-01T00:00:00Z')
            .ofNote(noteRealmAlpha)
            .please(),
        } as Awaited<
          ReturnType<typeof MemoryTrackerController.showMemoryTracker>
        >
      }
      if (id === 2) {
        return mt2Promise
      }
      throw new Error(`unexpected memoryTracker ${String(id)}`)
    })

    const { stdin, lastFrame, ...ink } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitRememberCard(ink, 'Alpha', { ynHint: true })
    stdin.write('y\r')

    await waitForLastFrame(
      lastFrame,
      (p) =>
        p.includes(RECALL_LOADING_NEXT_QUESTION_LABEL) && !p.includes('(y/n)')
    )

    resolveMt2({
      data: makeMe.aMemoryTracker
        .nextRecallAt('2026-06-01T00:00:00Z')
        .ofNote(noteRealmBeta)
        .please(),
    } as Awaited<ReturnType<typeof MemoryTrackerController.showMemoryTracker>>)

    await waitRememberCard(ink, 'Beta')
  })

  test('empty Enter and non-y/n on remember card do not recall; two-item session completes', async () => {
    setupTwoDueJustReviewItemsMocks()
    const markAsRecalledCount = mockMarkAsRecalledCounting()

    const { stdin, ...ink } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitRememberCard(ink, 'Alpha', { ynHint: true })
    await emptyEnterAndInvalidLineStayOnRemember(
      stdin,
      ink,
      'Alpha',
      'Recalled 2 notes',
      { skipInitialWait: true }
    )

    stdin.write('y\r')
    await waitRememberCard(ink, 'Beta')
    expect(markAsRecalledCount.n).toBe(1)

    stdin.write('y\r')
    await waitLoadMore(ink)
    stdin.write('n\r')
    await waitRecalledSummary(ink, 'Recalled 2 notes')
    expect(markAsRecalledCount.n).toBe(2)

    const out = ink.lastStrippedFrame()
    expect(out).toContain('body')
    expect(out).toContain('Reviewed: Alpha')
  })

  test('just-review answered block: breadcrumb folder › note, content, Reviewed line', async () => {
    mockMarkAsRecalledCounting()
    mockShowMemoryTrackerCardForRealm(childNoteUnderEnglish())

    const { stdin, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(stdin)
    await waitForFramesToInclude(/(?=.*Yes, I remember\?)(?=.*Sedition)/s)
    stdin.write('y\r')
    await waitForFramesToInclude(
      /(?=.*English › Sedition)(?=.*Sedition means incite violence)(?=.*Reviewed: Sedition)/s
    )
  })

  test('missing note title falls back to Note; empty content; no notebook line', async () => {
    mockShowMemoryTrackerCardForRealm(
      makeMe.aNoteRealm
        .title('   ')
        .content('')
        .createdAt(baseNoteTimes.createdAt)
        .updatedAt(baseNoteTimes.updatedAt)
        .please()
    )

    const { stdin, ...ink } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )
    startRecall(stdin)
    await ink.waitUntilLastFrame(
      (f) =>
        f.includes('Yes, I remember?') &&
        f.includes('Note') &&
        !f.includes('Alpha')
    )

    expect(ink.lastStrippedFrame()).toContain('Note')
    expect(ink.lastStrippedFrame()).not.toContain('Alpha')
    stdin.write('n\r')
    await ink.waitForLastFrameToInclude('Reduced memory index.')
  })

  test('two due just-review items: y twice then n on load more → Recalled 2 notes', async () => {
    setupTwoDueJustReviewItemsMocks()
    const markAsRecalledCount = mockMarkAsRecalledCounting()

    const { stdin, lastFrame, ...ink } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitRememberCard(ink, 'Alpha')
    stdin.write('y\r')
    await waitRememberCard(ink, 'Beta')
    await waitForLastFrame(lastFrame, (plain) => {
      return (
        plain.includes('Reviewed: Alpha') &&
        plain.includes('body') &&
        plain.includes('Alpha')
      )
    })
    stdin.write('y\r')
    await waitLoadMore(ink)
    stdin.write('n\r')
    await waitRecalledSummary(ink, 'Recalled 2 notes')
    expect(markAsRecalledCount.n).toBe(2)
    expect(api.recallingSpy).toHaveBeenCalledTimes(1)
  })
})
