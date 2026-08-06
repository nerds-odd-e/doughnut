import { describeRecallSpellingInteractive } from './recallSpellingInteractive.suite.js'

describeRecallSpellingInteractive((api) => {
  const {
    test,
    expect,
    InteractiveCliApp,
    leaveRecallWithYnRe,
    startRecall,
    renderInkWhenCommandLineReady,
    pressEscape,
    waitSpellingPromptVisible,
    waitReturnsToSpellingWithBuffer,
  } = api

  test('after Esc from spelling, y settles with Recall session stopped and never calls answerSpelling', async () => {
    const { stdin, ...ink } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitSpellingPromptVisible(ink)
    await pressEscape(stdin)
    await ink.waitForLastFrameToInclude(leaveRecallWithYnRe)

    stdin.write('y\r')
    await ink.waitForLastFrameToInclude(/Recall session stopped\./)

    expect(api.answerSpellingSpy).not.toHaveBeenCalled()
  })

  test('after Esc from spelling, n returns to spelling card without answerSpelling; buffer preserved', async () => {
    const { stdin, ...ink } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitSpellingPromptVisible(ink)

    stdin.write('par')
    await ink.waitUntilLastFrame((p) => p.includes('→ par'))

    await pressEscape(stdin)
    await ink.waitForLastFrameToInclude(/Leave recall\?/)

    stdin.write('n\r')
    await waitReturnsToSpellingWithBuffer(ink, '→ par')

    expect(api.answerSpellingSpy).not.toHaveBeenCalled()
  })

  test('empty Enter on spelling leave confirm stays on confirm; n returns with buffer', async () => {
    const { stdin, ...ink } = await renderInkWhenCommandLineReady(
      <InteractiveCliApp />
    )

    startRecall(stdin)
    await waitSpellingPromptVisible(ink)
    stdin.write('par')
    await ink.waitUntilLastFrame((p) => p.includes('→ par'))

    await pressEscape(stdin)
    await ink.waitForLastFrameToInclude(/Leave recall\?/)

    stdin.write('\r')
    await ink.waitForLastFrameToInclude(/Leave recall\?/)

    stdin.write('n\r')
    await waitReturnsToSpellingWithBuffer(ink, '→ par')

    expect(api.answerSpellingSpy).not.toHaveBeenCalled()
  })
})
