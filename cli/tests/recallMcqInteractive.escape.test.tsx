import { describeRecallMcqInteractive } from './recallMcqInteractive.suite.js'

describeRecallMcqInteractive((api) => {
  const {
    test,
    expect,
    InteractiveCliApp,
    leaveRecallWithYnRe,
    startRecall,
    renderInkWhenCommandLineReady,
    pressEscape,
    waitMcqVisible,
    waitMcqIncorrectOnLastFrame,
    waitReturnsToMcq,
    pendingMcqPrompt,
    mcqAnsweredPrompt,
    mockSingleMcqDue,
    RECALL_PROMPT_ID,
    RecallPromptController,
  } = api

  test('after Esc, y settles with Recall session stopped and never calls answer', async () => {
    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(ink.stdin)
    await waitMcqVisible(ink)
    await pressEscape(ink.stdin)
    await ink.waitForLastFrameToInclude(leaveRecallWithYnRe)
    ink.stdin.write('y\r')
    await ink.waitForLastFrameToInclude('Recall session stopped.')
    expect(api.answerSpy).not.toHaveBeenCalled()
  })

  test('after Esc, n returns to MCQ without answer; buffer preserved', async () => {
    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(ink.stdin)
    await waitMcqVisible(ink)
    ink.stdin.write('z')
    await ink.waitUntilLastFrame((p) => p.includes('→ z'))
    await pressEscape(ink.stdin)
    await ink.waitForLastFrameToInclude(/Leave recall\?/)
    ink.stdin.write('n\r')
    await ink.waitUntilLastFrame(
      (p) =>
        p.includes('→ z') &&
        p.includes('Choose') &&
        !p.includes('Leave recall?')
    )
    expect(api.answerSpy).not.toHaveBeenCalled()
  })

  test('after Esc then n, MCQ list highlight preserved (Enter submits second choice)', async () => {
    mockSingleMcqDue()
    const pending = pendingMcqPrompt()
    api.answerSpy.mockResolvedValue({
      data: mcqAnsweredPrompt(pending, {
        id: 100,
        correct: false,
        choiceIndex: 1,
      }),
    } as Awaited<ReturnType<typeof RecallPromptController.answer>>)

    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(ink.stdin)
    await waitMcqVisible(ink)
    ink.stdin.write('\u001b[B')
    await ink.waitUntilLastFrame((p) => p.includes('2.'))
    await pressEscape(ink.stdin)
    await ink.waitForLastFrameToInclude(/Leave recall\?/)
    ink.stdin.write('n\r')
    await waitReturnsToMcq(ink)
    ink.stdin.write('\r')
    await waitMcqIncorrectOnLastFrame(ink)
    expect(api.answerSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { recallPrompt: RECALL_PROMPT_ID },
        body: { choiceIndex: 1 },
      })
    )
  })

  test('empty Enter on leave recall confirm stays on confirm; n returns to MCQ', async () => {
    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(ink.stdin)
    await waitMcqVisible(ink)
    await pressEscape(ink.stdin)
    await ink.waitForLastFrameToInclude(/Leave recall\?/)
    ink.stdin.write('\r')
    await ink.waitForLastFrameToInclude(/Leave recall\?/)
    ink.stdin.write('n\r')
    await waitReturnsToMcq(ink)
    expect(api.answerSpy).not.toHaveBeenCalled()
  })
})
