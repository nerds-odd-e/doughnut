import { useEffect, useRef } from 'react'
import { render } from 'ink-testing-library'
import { describe, expect, test, vi } from 'vitest'
import { YesNoStagePrompt } from '../src/commonUIComponents/YesNoStagePrompt.js'
import {
  renderInkWhenCommandLineReady,
  StageKeyRoot,
  stripAnsi,
  waitForFrames,
  waitForLastFrame,
} from './inkTestHelpers.js'

const okAndDefaultYesHintRe = /(?=.*OK\?)(?=.*\(Y\/n\))/s

describe('YesNoStagePrompt', () => {
  test('empty Enter with defaultAnswer true calls onAnswer(true) and shows (Y/n)', async () => {
    const onAnswer = vi.fn()
    const { stdin, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(
        <StageKeyRoot>
          <YesNoStagePrompt
            prompt="OK?"
            onAnswer={onAnswer}
            defaultAnswer={true}
          />
        </StageKeyRoot>
      )

    await waitForFramesToInclude(okAndDefaultYesHintRe)

    stdin.write('\r')
    await waitForFrames(
      () => String(onAnswer.mock.calls.length),
      (c) => Number(c) >= 1
    )
    expect(onAnswer).toHaveBeenCalledWith(true)
  })

  test('empty Enter with defaultAnswer false calls onAnswer(false) and shows (y/N)', async () => {
    const onAnswer = vi.fn()
    const { stdin, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(
        <StageKeyRoot>
          <YesNoStagePrompt
            prompt="OK?"
            onAnswer={onAnswer}
            defaultAnswer={false}
          />
        </StageKeyRoot>
      )

    await waitForFramesToInclude(/\(y\/N\)/)

    stdin.write('\r')
    await waitForFrames(
      () => String(onAnswer.mock.calls.length),
      (c) => Number(c) >= 1
    )
    expect(onAnswer).toHaveBeenCalledWith(false)
  })

  test('whitespace-only buffer + Enter with default commits default', async () => {
    const onAnswer = vi.fn()
    const { stdin, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(
        <StageKeyRoot>
          <YesNoStagePrompt
            prompt="OK?"
            onAnswer={onAnswer}
            defaultAnswer={true}
          />
        </StageKeyRoot>
      )

    await waitForFramesToInclude(/OK\?/)

    stdin.write('  \t  \r')
    await waitForFrames(
      () => String(onAnswer.mock.calls.length),
      (c) => Number(c) >= 1
    )
    expect(onAnswer).toHaveBeenCalledWith(true)
  })

  test('empty Enter without default does not call onAnswer; y then commits yes', async () => {
    const onAnswer = vi.fn()
    const { stdin, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(
        <StageKeyRoot>
          <YesNoStagePrompt prompt="OK?" onAnswer={onAnswer} />
        </StageKeyRoot>
      )

    await waitForFramesToInclude(/\(y\/n\)/)

    stdin.write('\r')
    stdin.write('y\r')
    await waitForFrames(
      () => String(onAnswer.mock.calls.length),
      (c) => Number(c) >= 1
    )
    expect(onAnswer).toHaveBeenCalledTimes(1)
    expect(onAnswer).toHaveBeenCalledWith(true)
  })

  test('Escape calls onCancel when input is blocked', async () => {
    const onAnswer = vi.fn()
    const onCancel = vi.fn()
    function BlockedYesNo() {
      const blocked = useRef(false)
      useEffect(() => {
        blocked.current = true
      })
      return (
        <YesNoStagePrompt
          prompt="OK?"
          onAnswer={onAnswer}
          onCancel={onCancel}
          inputBlockedRef={blocked}
        />
      )
    }
    const { stdin, frames } = render(
      <StageKeyRoot>
        <BlockedYesNo />
      </StageKeyRoot>
    )

    await waitForFrames(
      () => stripAnsi(frames.join('\n')),
      (c) => c.includes('OK?')
    )
    let drain = 0
    await waitForFrames(
      () => String(++drain),
      (s) => Number(s) >= 30
    )

    stdin.write('\u001b')
    await waitForFrames(
      () => String(onCancel.mock.calls.length),
      (c) => Number(c) >= 1
    )
    expect(onCancel).toHaveBeenCalledTimes(1)
    expect(onAnswer).not.toHaveBeenCalled()
  })

  test('Escape calls onCancel when set', async () => {
    const onAnswer = vi.fn()
    const onCancel = vi.fn()
    const { stdin, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(
        <StageKeyRoot>
          <YesNoStagePrompt
            prompt="OK?"
            onAnswer={onAnswer}
            onCancel={onCancel}
          />
        </StageKeyRoot>
      )

    await waitForFramesToInclude(/OK\?/)

    stdin.write('\u001b')
    await waitForFrames(
      () => String(onCancel.mock.calls.length),
      (c) => Number(c) >= 1
    )
    expect(onCancel).toHaveBeenCalledTimes(1)
    expect(onAnswer).not.toHaveBeenCalled()
  })

  test('Escape without onCancel does not call onAnswer; y still commits after a turn drain', async () => {
    const onAnswer = vi.fn()
    const { stdin, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(
        <StageKeyRoot>
          <YesNoStagePrompt prompt="OK?" onAnswer={onAnswer} />
        </StageKeyRoot>
      )

    await waitForFramesToInclude(/OK\?/)

    stdin.write('\u001b')
    let drain = 0
    await waitForFrames(
      () => String(++drain),
      (s) => Number(s) >= 30
    )
    expect(onAnswer).not.toHaveBeenCalled()

    stdin.write('y\r')
    await waitForFrames(
      () => String(onAnswer.mock.calls.length),
      (c) => Number(c) >= 1
    )
    expect(onAnswer).toHaveBeenCalledTimes(1)
    expect(onAnswer).toHaveBeenCalledWith(true)
  })

  test('typed n commits no even with defaultAnswer true', async () => {
    const onAnswer = vi.fn()
    const { stdin, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(
        <StageKeyRoot>
          <YesNoStagePrompt
            prompt="OK?"
            onAnswer={onAnswer}
            defaultAnswer={true}
          />
        </StageKeyRoot>
      )

    await waitForFramesToInclude(/OK\?/)

    stdin.write('n\r')
    await waitForFrames(
      () => String(onAnswer.mock.calls.length),
      (c) => Number(c) >= 1
    )
    expect(onAnswer).toHaveBeenCalledWith(false)
  })

  test('non-y/n committed line does not call onAnswer', async () => {
    const onAnswer = vi.fn()
    const { stdin, lastFrame, waitForFramesToInclude } =
      await renderInkWhenCommandLineReady(
        <StageKeyRoot>
          <YesNoStagePrompt
            prompt="OK?"
            onAnswer={onAnswer}
            defaultAnswer={true}
          />
        </StageKeyRoot>
      )

    await waitForFramesToInclude(/OK\?/)

    stdin.write('x\r')
    expect(onAnswer).not.toHaveBeenCalled()

    stdin.write('\x7f')
    await waitForLastFrame(lastFrame, (f) => {
      const p = stripAnsi(f)
      return p.includes('→') && !p.includes('→ x') && !p.includes('→x')
    })

    stdin.write('y\r')
    await waitForFrames(
      () => String(onAnswer.mock.calls.length),
      (c) => Number(c) >= 1
    )
    expect(onAnswer).toHaveBeenCalledTimes(1)
    expect(onAnswer).toHaveBeenCalledWith(true)
  })
})
