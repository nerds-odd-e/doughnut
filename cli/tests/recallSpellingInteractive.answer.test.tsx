import { describeRecallSpellingInteractive } from './recallSpellingInteractive.suite.js'

describeRecallSpellingInteractive((api) => {
  const {
    test,
    expect,
    makeMe,
    MemoryTrackerController,
    RecallPromptController,
    RecallsController,
    InteractiveCliApp,
    SPELL_PROMPT_ID,
    SPELL_PROMPT_ID_2,
    SPELL_PLACEHOLDER_SUBSTR,
    deferred,
    startRecall,
    waitBusySubmitAnswer,
    waitLoadingSpellingNext,
    renderInkWhenCommandLineReady,
    waitSpellingPromptVisible,
    waitSpellingIncorrect,
    waitSpellingCorrect,
    pendingSpellingPrompt,
    spellingAnsweredPrompt,
    mockRecallingFirstThenEmpty,
  } = api

  test('wrong spelling shows Incorrect and records answer with correct false', async () => {
    mockRecallingFirstThenEmpty()

    const pending = pendingSpellingPrompt()
    api.answerSpellingSpy.mockResolvedValue({
      data: spellingAnsweredPrompt(pending, {
        correct: false,
        spellingAnswer: 'typo',
      }),
    } as Awaited<ReturnType<typeof RecallPromptController.answerSpelling>>)

    const { stdin, ...ink } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitSpellingPromptVisible(ink)

    stdin.write('typo\r')
    await waitSpellingIncorrect(ink, 'typo')

    expect(api.answerSpellingSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { recallPrompt: SPELL_PROMPT_ID },
        body: { spellingAnswer: 'typo' },
      })
    )
  })

  test('shows busy label in bordered input while answerSpelling is pending', async () => {
    mockRecallingFirstThenEmpty()

    const pending = pendingSpellingPrompt()
    const { promise: answerPromise, resolve: resolveAnswer } =
      deferred<
        Awaited<ReturnType<typeof RecallPromptController.answerSpelling>>
      >()
    api.answerSpellingSpy.mockImplementation(() => answerPromise)

    const { stdin, ...ink } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitSpellingPromptVisible(ink)

    stdin.write('typo\r')

    await waitBusySubmitAnswer(ink)

    resolveAnswer({
      data: spellingAnsweredPrompt(pending, {
        correct: false,
        spellingAnswer: 'typo',
      }),
    } as Awaited<ReturnType<typeof RecallPromptController.answerSpelling>>)

    await waitSpellingIncorrect(ink, 'typo')
  })

  test('after first spelling answer, shows loading spelling until second question loads', async () => {
    const secondStem = 'Second spell stem loading next unique'
    const pending1 = pendingSpellingPrompt()
    const pending2 = makeMe.aRecallPrompt
      .withId(SPELL_PROMPT_ID_2)
      .withSpellingStem(secondStem)
      .please()

    api.recallingSpy.mockResolvedValue({
      data: makeMe.aDueMemoryTrackersList
        .totalAssimilatedCount(0)
        .toRepeat([
          { memoryTrackerId: 1, spelling: true },
          { memoryTrackerId: 2, spelling: true },
        ])
        .please(),
    } as Awaited<ReturnType<typeof RecallsController.recalling>>)

    const { promise: ask2Promise, resolve: resolveAsk2 } =
      deferred<
        Awaited<ReturnType<typeof MemoryTrackerController.askAQuestion>>
      >()

    let askN = 0
    api.askAQuestionSpy.mockImplementation(() => {
      askN += 1
      if (askN === 1) {
        return Promise.resolve({
          data: pending1,
        } as Awaited<ReturnType<typeof MemoryTrackerController.askAQuestion>>)
      }
      if (askN === 2) {
        return ask2Promise
      }
      throw new Error(`unexpected askAQuestion call ${String(askN)}`)
    })

    api.answerSpellingSpy.mockResolvedValue({
      data: spellingAnsweredPrompt(pending1, {
        correct: false,
        spellingAnswer: 'typo',
      }),
    } as Awaited<ReturnType<typeof RecallPromptController.answerSpelling>>)

    const { stdin, ...ink } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitSpellingPromptVisible(ink)

    stdin.write('typo\r')

    await waitLoadingSpellingNext(ink, SPELL_PLACEHOLDER_SUBSTR)

    resolveAsk2({
      data: pending2,
    } as Awaited<ReturnType<typeof MemoryTrackerController.askAQuestion>>)

    await ink.waitForLastFrameToInclude(secondStem)
  })

  test('submitted spelling trims NBSP and preserves mixed case in API body', async () => {
    mockRecallingFirstThenEmpty()

    const pending = pendingSpellingPrompt()
    api.answerSpellingSpy.mockImplementation((opts) => {
      expect(opts.body.spellingAnswer).toBe('SeDiTiOn')
      return Promise.resolve({
        data: spellingAnsweredPrompt(pending, {
          correct: true,
          spellingAnswer: 'SeDiTiOn',
        }),
      } as Awaited<ReturnType<typeof RecallPromptController.answerSpelling>>)
    })

    const { stdin, ...ink } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitSpellingPromptVisible(ink)

    stdin.write('\u00A0SeDiTiOn\u00A0\r')
    await waitSpellingCorrect(ink, 'SeDiTiOn')

    expect(api.answerSpellingSpy).toHaveBeenCalledTimes(1)
  })
})
