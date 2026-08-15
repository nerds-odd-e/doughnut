import { describeRecallMcqInteractive } from './recallMcqInteractive.suite.js'
import { reLiteral } from './recallInteractiveShared.js'

describeRecallMcqInteractive((api) => {
  const {
    test,
    expect,
    makeMe,
    MemoryTrackerController,
    RecallPromptController,
    RecallsController,
    InteractiveCliApp,
    RECALL_PROMPT_ID,
    deferred,
    startRecall,
    waitBusySubmitAnswer,
    waitLoadingNextQuestion,
    renderInkWhenCommandLineReady,
    waitMcqVisible,
    waitMcqLoadMore,
    waitMcqIncorrectOnLastFrame,
    pendingMcqPrompt,
    mcqAnsweredPrompt,
    mockSingleMcqDue,
  } = api

  test('wrong MCQ choice shows Incorrect and sends 0-based choiceIndex to API', async () => {
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
    ink.stdin.write('2\r')
    await waitMcqIncorrectOnLastFrame(ink)
    await waitMcqLoadMore(ink)

    expect(ink.lastStrippedFrame()).toContain('Beta')
    expect(api.answerSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { recallPrompt: RECALL_PROMPT_ID },
        body: { choiceIndex: 1 },
      })
    )
  })

  test('shows busy label in bordered input while answer is pending', async () => {
    mockSingleMcqDue()
    const pending = pendingMcqPrompt()
    const { promise: answerPromise, resolve: resolveAnswer } =
      deferred<Awaited<ReturnType<typeof RecallPromptController.answer>>>()
    api.answerSpy.mockImplementation(() => answerPromise)

    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(ink.stdin)
    await waitMcqVisible(ink)
    ink.stdin.write('2\r')
    await waitBusySubmitAnswer(ink)

    resolveAnswer({
      data: mcqAnsweredPrompt(pending, {
        id: 100,
        correct: false,
        choiceIndex: 1,
      }),
    } as Awaited<ReturnType<typeof RecallPromptController.answer>>)

    await waitMcqLoadMore(ink)
  })

  test('after first MCQ answer, shows loading next label until second tracker loads', async () => {
    const SECOND_PROMPT_ID = 99
    const secondStem = 'SECOND_MCQ_LOADING_NEXT_UNIQUE'
    const pending = pendingMcqPrompt()
    const secondPrompt = makeMe.aRecallPromptHistoryItem
      .withId(SECOND_PROMPT_ID)
      .withQuestionStem(secondStem)
      .withChoices(['X', 'Y', 'Z'])
      .please()

    api.recallingSpy.mockResolvedValue({
      data: makeMe.aDueMemoryTrackersList
        .totalAssimilatedCount(0)
        .toRepeat([
          { memoryTrackerId: 1, spelling: false as const },
          { memoryTrackerId: 2, spelling: false as const },
        ])
        .please(),
    } as Awaited<ReturnType<typeof RecallsController.recalling>>)

    const { promise: prompts2Promise, resolve: resolvePrompts2 } =
      deferred<
        Awaited<ReturnType<typeof MemoryTrackerController.getRecallPrompts>>
      >()

    api.getRecallPromptsSpy.mockImplementation((opts) => {
      const id = opts.path.memoryTracker
      if (id === 1) {
        return Promise.resolve({
          data: [pending],
        } as Awaited<
          ReturnType<typeof MemoryTrackerController.getRecallPrompts>
        >)
      }
      if (id === 2) {
        return prompts2Promise
      }
      throw new Error(`unexpected memoryTracker ${String(id)}`)
    })

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
    ink.stdin.write('2\r')

    await waitLoadingNextQuestion(ink)

    resolvePrompts2({
      data: [secondPrompt],
    } as Awaited<ReturnType<typeof MemoryTrackerController.getRecallPrompts>>)

    await ink.waitForLastFrameToInclude(secondStem)
  })

  test('out-of-range MCQ number does not call answer; valid answer still works', async () => {
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
    ink.stdin.write('9\r')
    await ink.waitForLastFrameToInclude('→ 9')
    expect(api.answerSpy).not.toHaveBeenCalled()

    ink.stdin.write('\x7f2\r')
    await waitMcqIncorrectOnLastFrame(ink)
    expect(api.answerSpy).toHaveBeenCalledTimes(1)
  })

  test('wrong MCQ then next due tracker shows second question without ending recall', async () => {
    const SECOND_PROMPT_ID = 99
    const secondStem = 'SECOND_MCQ_STEM_UNIQUE'
    const pending = pendingMcqPrompt()
    const secondPrompt = makeMe.aRecallPromptHistoryItem
      .withId(SECOND_PROMPT_ID)
      .withQuestionStem(secondStem)
      .withChoices(['X', 'Y', 'Z'])
      .please()

    const note2 = makeMe.aNoteRealm.title('Beta').content('body2').please()

    api.recallingSpy.mockResolvedValue({
      data: makeMe.aDueMemoryTrackersList
        .totalAssimilatedCount(0)
        .toRepeat([
          { memoryTrackerId: 1, spelling: false as const },
          { memoryTrackerId: 2, spelling: false as const },
        ])
        .please(),
    } as Awaited<ReturnType<typeof RecallsController.recalling>>)

    api.getRecallPromptsSpy.mockImplementation((opts) => {
      const id = opts.path.memoryTracker
      if (id === 1) {
        return Promise.resolve({
          data: [pending],
        } as Awaited<
          ReturnType<typeof MemoryTrackerController.getRecallPrompts>
        >)
      }
      if (id === 2) {
        return Promise.resolve({
          data: [secondPrompt],
        } as Awaited<
          ReturnType<typeof MemoryTrackerController.getRecallPrompts>
        >)
      }
      throw new Error(`unexpected memoryTracker ${String(id)}`)
    })

    let answerN = 0
    api.answerSpy.mockImplementation(() => {
      answerN += 1
      if (answerN === 1) {
        return Promise.resolve({
          data: mcqAnsweredPrompt(pending, {
            id: 100,
            correct: false,
            choiceIndex: 1,
          }),
        } as Awaited<ReturnType<typeof RecallPromptController.answer>>)
      }
      return Promise.resolve({
        data: makeMe.anAnsweredQuestion
          .fromMcqHistoryItem(secondPrompt, note2.note, 2)
          .withAnswer({ id: 101, correct: true, choiceIndex: 0 })
          .please(),
      } as Awaited<ReturnType<typeof RecallPromptController.answer>>)
    })

    const ink = await renderInkWhenCommandLineReady(<InteractiveCliApp />)

    startRecall(ink.stdin)
    await waitMcqVisible(ink)
    ink.stdin.write('2\r')
    await ink.waitForLastFrameToInclude(
      new RegExp(`(?=.*Incorrect)(?=.*${reLiteral(secondStem)})`, 's')
    )
    expect(api.answerSpy).toHaveBeenCalledTimes(1)

    ink.stdin.write('1\r')
    await ink.waitForLastFrameToInclude('Correct!')
    expect(ink.lastStrippedFrame()).toContain(secondStem)
    expect(api.answerSpy).toHaveBeenCalledTimes(2)
    expect(api.recallingSpy).toHaveBeenCalledTimes(1)
  })
})
