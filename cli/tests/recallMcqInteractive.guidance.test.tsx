import { describeRecallMcqInteractive } from './recallMcqInteractive.suite.js'

describeRecallMcqInteractive((api) => {
  const {
    test,
    expect,
    pendingMcqQuestion,
    InteractiveCliApp,
    RECALL_PROMPT_ID,
    EXPECT_GUIDANCE_MORE_BELOW,
    startRecall,
    renderInkWhenCommandLineReady,
  } = api

  test('many MCQ choices use a fixed-height list with more-below', async () => {
    const manyChoicesCount = 15
    api.setMcqQuestion(
      1,
      pendingMcqQuestion(
        RECALL_PROMPT_ID,
        'Pick one',
        Array.from({ length: manyChoicesCount }, (_, i) => `c${i}`)
      )
    )

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
