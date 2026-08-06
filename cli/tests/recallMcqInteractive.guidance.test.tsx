import { describeRecallMcqInteractive } from './recallMcqInteractive.suite.js'

describeRecallMcqInteractive((api) => {
  const {
    test,
    expect,
    makeMe,
    MemoryTrackerController,
    InteractiveCliApp,
    RECALL_PROMPT_ID,
    EXPECT_GUIDANCE_MORE_BELOW,
    startRecall,
    renderInkWhenCommandLineReady,
  } = api

  test('many MCQ choices use a fixed-height list with more-below', async () => {
    const manyChoicesCount = 15
    api.getRecallPromptsSpy.mockResolvedValue({
      data: [
        makeMe.aRecallPrompt
          .withId(RECALL_PROMPT_ID)
          .withQuestionStem('Pick one')
          .withChoices(
            Array.from({ length: manyChoicesCount }, (_, i) => `c${i}`)
          )
          .please(),
      ],
    } as Awaited<ReturnType<typeof MemoryTrackerController.getRecallPrompts>>)

    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(ink.stdin)
    await ink.waitUntilLastFrame(
      (p) =>
        p.includes('Pick one') &&
        p.includes(EXPECT_GUIDANCE_MORE_BELOW) &&
        p.includes('1. c0')
    )

    expect(ink.lastStrippedFrame()).not.toMatch(
      new RegExp(`${manyChoicesCount}\\.\\s*c${manyChoicesCount - 1}`)
    )
  })
})
