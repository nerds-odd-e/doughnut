import { describeRecallJustReviewInteractive } from './recallJustReviewInteractive.suite.js'
import {
  waitBusyRecordReview,
  waitLoadingNextQuestion,
} from './recallInteractiveShared.js'

describeRecallJustReviewInteractive((api) => {
  const {
    test,
    expect,
    makeMe,
    MemoryTrackerController,
    RecallsController,
    InteractiveCliApp,
    renderInkWhenCommandLineReady,
    waitForLastFrame,
    waitRememberCard,
    mockDeferredMarkAsRecalled,
    mockShowMemoryTrackerSecondCardDelayed,
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
    const { resolveMark } = mockDeferredMarkAsRecalled()

    const { stdin, ...ink } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitRememberCard(ink, 'Alpha', { ynHint: true })
    stdin.write('y\r')

    await waitBusyRecordReview(ink)

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

    const { resolveSecondCard } = mockShowMemoryTrackerSecondCardDelayed(
      noteRealmAlpha,
      noteRealmBeta
    )

    const { stdin, ...ink } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitRememberCard(ink, 'Alpha', { ynHint: true })
    stdin.write('y\r')

    await waitLoadingNextQuestion(ink)

    resolveSecondCard({
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
