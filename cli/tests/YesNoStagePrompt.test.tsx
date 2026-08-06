import { useEffect, useRef } from 'react'
import { render } from 'ink-testing-library'
import { describe, expect, test, vi } from 'vitest'
import { YesNoStagePrompt } from '../src/commonUIComponents/YesNoStagePrompt.js'
import {
  pressEscapeAndWait,
  renderInkWhenCommandLineReady,
  StageKeyRoot,
  stripAnsi,
  waitForFrames,
  waitForLastFrame,
} from './inkTestHelpers.js'

const okAndDefaultYesHintRe = /(?=.*OK\?)(?=.*\(Y\/n\))/s

type YesNoHandlers = {
  onAnswer: ReturnType<typeof vi.fn>
  onCancel?: ReturnType<typeof vi.fn>
  defaultAnswer?: boolean
}

async function renderYesNo(props: YesNoHandlers) {
  return renderInkWhenCommandLineReady(
    <StageKeyRoot>
      <YesNoStagePrompt prompt="OK?" {...props} />
    </StageKeyRoot>
  )
}

async function waitUntilAnswered(onAnswer: ReturnType<typeof vi.fn>) {
  await waitForFrames(
    () => String(onAnswer.mock.calls.length),
    (c) => Number(c) >= 1
  )
}

describe('YesNoStagePrompt', () => {
  test('empty Enter with defaultAnswer true calls onAnswer(true) and shows (Y/n)', async () => {
    const onAnswer = vi.fn()
    const { stdin, waitForFramesToInclude } = await renderYesNo({
      onAnswer,
      defaultAnswer: true,
    })

    await waitForFramesToInclude(okAndDefaultYesHintRe)

    stdin.write('\r')
    await waitUntilAnswered(onAnswer)
    expect(onAnswer).toHaveBeenCalledWith(true)
  })

  test('empty Enter with defaultAnswer false calls onAnswer(false) and shows (y/N)', async () => {
    const onAnswer = vi.fn()
    const { stdin, waitForFramesToInclude } = await renderYesNo({
      onAnswer,
      defaultAnswer: false,
    })

    await waitForFramesToInclude(/\(y\/N\)/)

    stdin.write('\r')
    await waitUntilAnswered(onAnswer)
    expect(onAnswer).toHaveBeenCalledWith(false)
  })

  test('whitespace-only buffer + Enter with default commits default', async () => {
    const onAnswer = vi.fn()
    const { stdin, waitForFramesToInclude } = await renderYesNo({
      onAnswer,
      defaultAnswer: true,
    })

    await waitForFramesToInclude(/OK\?/)

    stdin.write('  \t  \r')
    await waitUntilAnswered(onAnswer)
    expect(onAnswer).toHaveBeenCalledWith(true)
  })

  test('empty Enter without default does not call onAnswer; y then commits yes', async () => {
    const onAnswer = vi.fn()
    const { stdin, waitForFramesToInclude } = await renderYesNo({ onAnswer })

    await waitForFramesToInclude(/\(y\/n\)/)

    stdin.write('\r')
    stdin.write('y\r')
    await waitUntilAnswered(onAnswer)
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

    await pressEscapeAndWait(
      stdin,
      () => String(onCancel.mock.calls.length),
      (c) => Number(c) >= 1
    )
    expect(onCancel).toHaveBeenCalledTimes(1)
    expect(onAnswer).not.toHaveBeenCalled()
  })

  test('Escape calls onCancel when set', async () => {
    const onAnswer = vi.fn()
    const onCancel = vi.fn()
    const { stdin, waitForFramesToInclude } = await renderYesNo({
      onAnswer,
      onCancel,
    })

    await waitForFramesToInclude(/OK\?/)

    await pressEscapeAndWait(
      stdin,
      () => String(onCancel.mock.calls.length),
      (c) => Number(c) >= 1
    )
    expect(onCancel).toHaveBeenCalledTimes(1)
    expect(onAnswer).not.toHaveBeenCalled()
  })

  test('Escape without onCancel does not call onAnswer; y still commits after a turn drain', async () => {
    // Ink 7 defers a lone ESC ~20ms; the next stdin chunk cancels that timer and merges bytes (breaks y).
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] })
    try {
      const onAnswer = vi.fn()
      const { stdin, waitForFramesToInclude } = await renderYesNo({ onAnswer })

      await waitForFramesToInclude(/OK\?/)

      stdin.write('\u001b')
      await vi.advanceTimersByTimeAsync(25)
      let drain = 0
      await waitForFrames(
        () => String(++drain),
        (s) => Number(s) >= 30
      )
      expect(onAnswer).not.toHaveBeenCalled()

      stdin.write('y\r')
      await waitUntilAnswered(onAnswer)
      expect(onAnswer).toHaveBeenCalledTimes(1)
      expect(onAnswer).toHaveBeenCalledWith(true)
    } finally {
      vi.useRealTimers()
    }
  })

  test('typed n commits no even with defaultAnswer true', async () => {
    const onAnswer = vi.fn()
    const { stdin, waitForFramesToInclude } = await renderYesNo({
      onAnswer,
      defaultAnswer: true,
    })

    await waitForFramesToInclude(/OK\?/)

    stdin.write('n\r')
    await waitUntilAnswered(onAnswer)
    expect(onAnswer).toHaveBeenCalledWith(false)
  })

  test('non-y/n committed line does not call onAnswer', async () => {
    const onAnswer = vi.fn()
    const { stdin, lastFrame, waitForFramesToInclude } = await renderYesNo({
      onAnswer,
      defaultAnswer: true,
    })

    await waitForFramesToInclude(/OK\?/)

    stdin.write('x\r')
    expect(onAnswer).not.toHaveBeenCalled()

    stdin.write('\x7f')
    await waitForLastFrame(lastFrame, (p) => {
      return p.includes('→') && !p.includes('→ x') && !p.includes('→x')
    })

    stdin.write('y\r')
    await waitUntilAnswered(onAnswer)
    expect(onAnswer).toHaveBeenCalledTimes(1)
    expect(onAnswer).toHaveBeenCalledWith(true)
  })
})
