import { describeRecallMcqInteractive } from './recallMcqInteractive.suite.js'
import { reLiteral } from './recallInteractiveShared.js'

describeRecallMcqInteractive((api) => {
  const {
    test,
    expect,
    makeMe,
    RecallPromptController,
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
    pendingMcqQuestion,
    mcqAnsweredPrompt,
    setDueMcqTrackers,
  } = api

  test('wrong MCQ choice shows Incorrect and sends 0-based choiceIndex to API', async () => {
    const pending = pendingMcqQuestion()
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
    const pending = pendingMcqQuestion()
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
    const pending = pendingMcqQuestion()
    const secondPrompt = pendingMcqQuestion(SECOND_PROMPT_ID, secondStem, [
      'X',
      'Y',
      'Z',
    ])

    setDueMcqTrackers(1, 2)

    const secondQuestion = deferred<typeof secondPrompt>()
    api.setMcqQuestion(2, secondQuestion.promise)

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

    secondQuestion.resolve(secondPrompt)

    await ink.waitForLastFrameToInclude(secondStem)
  })

  test('out-of-range MCQ number does not call answer; valid answer still works', async () => {
    const pending = pendingMcqQuestion()
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
    const pending = pendingMcqQuestion()
    const secondPrompt = pendingMcqQuestion(SECOND_PROMPT_ID, secondStem, [
      'X',
      'Y',
      'Z',
    ])

    const note2 = makeMe.aNoteRealm.title('Beta').content('body2').please()

    setDueMcqTrackers(1, 2)

    api.setMcqQuestion(2, secondPrompt)

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
          .fromMcqPrompt(secondPrompt, note2.note, 2)
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
