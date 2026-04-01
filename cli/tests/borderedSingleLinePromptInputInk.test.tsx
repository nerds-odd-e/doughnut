import { render } from 'ink-testing-library'
import { describe, expect, test } from 'vitest'
import { BorderedSingleLinePromptInputInk } from '../src/borderedSingleLinePromptInputInk.js'
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
})
