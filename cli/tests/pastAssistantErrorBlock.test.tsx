import { render } from 'ink-testing-library'
import { describe, expect, test } from 'vitest'
import { PastAssistantErrorBlock } from '../src/commonUIComponents/pastAssistantErrorBlock.js'
import { stripAnsi } from './inkTestHelpers.js'

describe('PastAssistantErrorBlock', () => {
  test('red background (41m), blank padded row above and below the text', () => {
    const { lastFrame } = render(
      <PastAssistantErrorBlock text="unsupported command" />
    )
    const raw = lastFrame() ?? ''
    expect(raw).toContain('\x1b[41m')
    expect(raw).toContain('unsupported command')
    const lines = stripAnsi(raw).split('\n')
    const i = lines.findIndex((l) => l.includes('unsupported command'))
    expect(i).toBeGreaterThanOrEqual(0)
    expect(lines[i]?.startsWith(' ')).toBe(true)
    expect(lines[i]?.trim()).toBe('unsupported command')
    expect(lines[i - 1]?.trim()).toBe('')
    expect(lines[i + 1]?.trim()).toBe('')
    expect(lines[i - 1]?.length).toBe(lines[i]?.length)
    expect(lines[i + 1]?.length).toBe(lines[i]?.length)
  })

  test('renders every newline as a separate red content row', () => {
    const { lastFrame } = render(
      <PastAssistantErrorBlock text={'Traceback:\n  File "x.py", line 1'} />
    )
    const raw = stripAnsi(lastFrame() ?? '')
    expect(raw).toContain('Traceback:')
    expect(raw).toContain('File "x.py", line 1')
    expect(raw.indexOf('Traceback:')).toBeLessThan(raw.indexOf('File "x.py"'))
  })
})
