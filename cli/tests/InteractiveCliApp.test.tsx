import { render } from 'ink-testing-library'
import { describe, test, expect } from 'vitest'
import { InteractiveCliApp } from '../src/InteractiveCliApp.js'
import { formatVersionOutput } from '../src/commands/version.js'

describe('InteractiveCliApp (ink-testing-library)', () => {
  test('shows version in the first frame', () => {
    const { lastFrame } = render(<InteractiveCliApp />)
    expect(lastFrame()).toContain(formatVersionOutput())
  })

  test('submitting /exit records it in output', async () => {
    const { lastFrame, stdin, frames } = render(<InteractiveCliApp />)
    expect(lastFrame()).toContain(formatVersionOutput())

    await new Promise<void>((resolve) => {
      setTimeout(resolve, 50)
    })

    for (const ch of '/exit') {
      stdin.write(ch)
      await new Promise<void>((r) => {
        setImmediate(r)
      })
    }
    stdin.write('\r')
    await new Promise<void>((resolve) => {
      setTimeout(resolve, 50)
    })

    const combined = frames.join('\n')
    expect(combined).toContain('/exit')
  })
})
