import { render } from 'ink-testing-library'
import { describe, expect, test } from 'vitest'
import { BorderedSingleLinePromptInputInk } from '../src/commonUIComponents/borderedSingleLinePromptInputInk.js'
import { stripAnsi } from './inkTestHelpers.js'

const MCQ_HINT =
  '↑↓ Enter or number to select; Esc asks to leave recall (y/n confirm)'

describe('BorderedSingleLinePromptInputInk', () => {
  test('narrow terminal truncates placeholder with ellipsis', () => {
    const { lastFrame } = render(
      <BorderedSingleLinePromptInputInk
        terminalColumns={28}
        buffer=""
        caretOffset={0}
        placeholder={MCQ_HINT}
      />
    )
    const plain = stripAnsi(lastFrame() ?? '')
    expect(plain).toContain('…')
    expect(plain).not.toContain('(y/n confirm)')
  })

  test('wide terminal shows full placeholder', () => {
    const { lastFrame } = render(
      <BorderedSingleLinePromptInputInk
        terminalColumns={120}
        buffer=""
        caretOffset={0}
        placeholder={MCQ_HINT}
      />
    )
    const plain = stripAnsi(lastFrame() ?? '')
    expect(plain).toContain(MCQ_HINT)
    expect(plain).not.toContain('…')
  })

  test('non-empty buffer omits placeholder', () => {
    const { lastFrame } = render(
      <BorderedSingleLinePromptInputInk
        terminalColumns={40}
        buffer="x"
        caretOffset={1}
        placeholder={MCQ_HINT}
      />
    )
    const plain = stripAnsi(lastFrame() ?? '')
    expect(plain).toContain('x')
    expect(plain).not.toContain('Enter or number')
  })

  test('busyLabel shows label and hides buffer and placeholder', () => {
    const busy = 'Submitting answer…'
    const { lastFrame } = render(
      <BorderedSingleLinePromptInputInk
        terminalColumns={80}
        buffer="should-hide"
        caretOffset={5}
        placeholder={MCQ_HINT}
        busyLabel={busy}
      />
    )
    const plain = stripAnsi(lastFrame() ?? '')
    expect(plain).toContain(busy)
    expect(plain).not.toContain('should-hide')
    expect(plain).not.toContain('Enter or number')
  })

  test('narrow terminal truncates busy label with ellipsis', () => {
    const longBusy =
      'Submitting a very long answer label that must be truncated here'
    const { lastFrame } = render(
      <BorderedSingleLinePromptInputInk
        terminalColumns={28}
        buffer=""
        caretOffset={0}
        placeholder=""
        busyLabel={longBusy}
      />
    )
    const plain = stripAnsi(lastFrame() ?? '')
    expect(plain).toContain('…')
  })
})
