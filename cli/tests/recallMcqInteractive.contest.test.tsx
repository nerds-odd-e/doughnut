import { describeRecallMcqInteractive } from './recallMcqInteractive.suite.js'

describeRecallMcqInteractive((api) => {
  const {
    test,
    expect,
    vi,
    InteractiveCliApp,
    RecallPromptController,
    startRecall,
    renderInkWhenCommandLineReady,
    waitMcqVisible,
    pendingMcqQuestion,
    setContestSpy,
    setRegenerateSpy,
  } = api

  test('rejected contest shows advice in session strip and does not call regenerate', async () => {
    const rejectAdvice = 'UNIQUE_REJECT_ADVICE_9_3'
    setContestSpy(
      vi.spyOn(RecallPromptController, 'contest').mockResolvedValue({
        data: { rejected: true, advice: rejectAdvice },
      } as Awaited<ReturnType<typeof RecallPromptController.contest>>)
    )
    const regenerateSpy = vi
      .spyOn(RecallPromptController, 'regenerate')
      .mockResolvedValue({
        data: pendingMcqQuestion(),
      } as Awaited<ReturnType<typeof RecallPromptController.regenerate>>)
    setRegenerateSpy(regenerateSpy)

    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(ink.stdin)
    await waitMcqVisible(ink)
    ink.stdin.write('/contest\r')
    await ink.waitForLastFrameToInclude(rejectAdvice)
    expect(ink.lastStrippedFrame()).toContain('Choose')
    expect(regenerateSpy).not.toHaveBeenCalled()
    expect(api.answerSpy).not.toHaveBeenCalled()
  })

  test('contest API error settles with user-visible message and leaves recall', async () => {
    setContestSpy(
      vi
        .spyOn(RecallPromptController, 'contest')
        .mockRejectedValue(new Error('contest failed hard'))
    )

    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(ink.stdin)
    await waitMcqVisible(ink)
    ink.stdin.write('/contest\r')
    await ink.waitForLastFrameToInclude('Donut service is not available')
    expect(api.answerSpy).not.toHaveBeenCalled()
  })
})
