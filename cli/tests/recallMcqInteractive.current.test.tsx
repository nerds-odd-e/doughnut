import { MemoryTrackerController, type RecallPromptController } from 'donut-api'
import { describeRecallMcqInteractive } from './recallMcqInteractive.suite.js'

describeRecallMcqInteractive((api) => {
  const { test, expect, vi, makeMe, InteractiveCliApp, pendingMcqQuestion } =
    api

  test('opens the current question and submits the choice from the displayed prompt', async ({
    onTestFinished,
  }) => {
    const history = vi.spyOn(MemoryTrackerController, 'getRecallPrompts')
    onTestFinished(() => {
      history.mockRestore()
    })
    history.mockResolvedValue({
      data: [
        makeMe.aRecallPromptHistoryItem
          .withId(41)
          .withQuestionStem('The observatory opens at nine. Which door?')
          .withChoices(['West', 'South'])
          .please(),
      ],
    } as Awaited<ReturnType<typeof MemoryTrackerController.getRecallPrompts>>)

    const selected = pendingMcqQuestion(
      42,
      'The observatory opens at ten. Which door?',
      ['East', 'North']
    )
    api.setMcqQuestion(1, selected)
    api.answerSpy.mockResolvedValue({
      data: api.mcqAnsweredPrompt(selected, {
        id: 100,
        correct: true,
        choiceIndex: 1,
      }),
    } as Awaited<ReturnType<typeof RecallPromptController.answer>>)

    const ink = await api.renderInkWhenCommandLineReady(<InteractiveCliApp />)
    api.startRecall(ink.stdin)
    await ink.waitForLastFrameToInclude(/The observatory opens at/)
    expect(ink.lastStrippedFrame()).toContain('opens at ten')

    api.setMcqQuestion(
      1,
      pendingMcqQuestion(43, 'The observatory opens at eleven. Which door?', [
        'West',
        'South',
      ])
    )
    ink.stdin.write('2')
    await ink.waitForLastFrameToInclude('→ 2')
    expect(ink.lastStrippedFrame()).toContain('North')
    ink.stdin.write('\r')
    await ink.waitForLastFrameToInclude('Correct!')
    expect(api.answerSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { recallPrompt: 42 },
        body: { choiceIndex: 1 },
      })
    )
  })
})
