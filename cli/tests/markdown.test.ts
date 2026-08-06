import { describe, test, expect } from 'vitest'
import { renderMarkdownToTerminal } from '../src/markdown.js'
import { stripAnsi } from './inkTestHelpers.js'

describe('renderMarkdownToTerminal', () => {
  test.each(['', '   \n  ', null])('empty for %j', (input) => {
    expect(renderMarkdownToTerminal(input as never)).toBe('')
  })

  test('markdown emphasis and headings emit ANSI without raw markers', () => {
    const bold = renderMarkdownToTerminal('**bold**')
    expect(bold).toContain('\x1b[')
    expect(bold).not.toContain('**')

    const heading = renderMarkdownToTerminal('# Heading')
    expect(heading).toContain('\x1b[')
    expect(stripAnsi(heading)).toContain('Heading')
  })

  test('lists emit ANSI', () => {
    expect(renderMarkdownToTerminal('- item 1\n- item 2')).toContain('\x1b[')
  })

  test('wraps when width is provided', () => {
    expect(
      renderMarkdownToTerminal('a b c d e f g h i j k l m n o p', 10)
    ).toContain('\n')
  })

  test('HTML tags render as ANSI or plain text, never raw tags', () => {
    const bold = renderMarkdownToTerminal('<b>bold</b>')
    expect(bold).toContain('\x1b[')
    expect(bold).not.toContain('<b>')

    const mark = renderMarkdownToTerminal('<mark>[..~]</mark>')
    expect(mark).toContain('\x1b[')
    expect(mark).not.toContain('<mark>')

    const para = renderMarkdownToTerminal('<p>text</p>')
    expect(para).not.toContain('<p>')
    expect(para).toContain('text')
  })

  test('mixed markdown and inline HTML keeps visible text without escaped markers', () => {
    const mixed = renderMarkdownToTerminal('**bold** and <mark>m</mark>')
    expect(mixed).toContain('\x1b[')
    expect(mixed).not.toContain('<mark>')
    expect(mixed).not.toContain('\\*')
    const plain = stripAnsi(mixed)
    expect(plain).toContain('bold')
    expect(plain).toContain('m')
    expect(plain).not.toContain('**')

    const heading = renderMarkdownToTerminal('# Heading with <mark>x</mark>')
    expect(heading).not.toContain('<mark>')
    expect(heading).not.toContain('\\#')
    const headingPlain = stripAnsi(heading)
    expect(headingPlain).toContain('Heading with')
    expect(headingPlain).toContain('x')
  })
})
