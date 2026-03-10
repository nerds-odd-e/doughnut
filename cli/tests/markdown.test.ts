import { describe, test, expect } from 'vitest'
import { renderMarkdownToTerminal } from '../src/markdown.js'

describe('renderMarkdownToTerminal', () => {
  test('renders empty string as empty', () => {
    expect(renderMarkdownToTerminal('')).toBe('')
  })

  test('renders whitespace-only as empty', () => {
    expect(renderMarkdownToTerminal('   \n  ')).toBe('')
  })

  test('renders null/undefined as empty', () => {
    expect(renderMarkdownToTerminal(null as never)).toBe('')
  })

  test('outputs ANSI for bold', () => {
    const result = renderMarkdownToTerminal('**bold**')
    expect(result).toContain('\x1b[')
    expect(result).not.toContain('**')
  })

  test('outputs ANSI for headings', () => {
    const result = renderMarkdownToTerminal('# Heading')
    expect(result).toContain('\x1b[')
  })

  test('outputs ANSI for lists', () => {
    const result = renderMarkdownToTerminal('- item 1\n- item 2')
    expect(result).toContain('\x1b[')
  })

  test('uses provided width when given', () => {
    const narrow = renderMarkdownToTerminal(
      'a b c d e f g h i j k l m n o p',
      10
    )
    expect(narrow).toContain('\n')
  })
})
