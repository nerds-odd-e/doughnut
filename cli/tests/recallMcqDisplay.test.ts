import { describe, expect, test } from 'vitest'
import {
  recallMcqCurrentGuidanceLines,
  recallMcqNumberedChoiceLines,
  recallMcqStemWrappedLinesForCurrentPrompt,
} from '../src/recallMcqDisplay.js'
import { stripAnsi } from '../src/renderer.js'

describe('recall MCQ terminal display', () => {
  test('numbered choice lines: one row per API choice; embedded newlines in a choice do not add extra rows', () => {
    const lines = recallMcqNumberedChoiceLines(['line1\n\nline2', 'second'])
    expect(lines).toHaveLength(2)
    expect(lines.every((l) => stripAnsi(l).trim().length > 0)).toBe(true)
    expect(stripAnsi(lines[0]!)).toMatch(/^ {2}1\. /)
    expect(stripAnsi(lines[0]!)).toContain('line1')
    expect(stripAnsi(lines[0]!)).toContain('line2')
    expect(stripAnsi(lines[1]!)).toBe('  2. second')
  })

  test('Current guidance: exactly one highlighted row for the selected choice index', () => {
    const out = recallMcqCurrentGuidanceLines(['a', 'b', 'c'], 1, 120)
    const highlighted = out.filter((l) => l.includes('\x1b[7m'))
    expect(highlighted).toHaveLength(1)
    expect(stripAnsi(highlighted[0]!)).toMatch(/ {2}2\. /)
  })

  test('stem for Current prompt: empty markdown paragraph becomes a blank row between wrapped segments', () => {
    const lines = recallMcqStemWrappedLinesForCurrentPrompt('hi\n\nthere', 80)
    expect(lines[0]).toBe('hi')
    expect(lines[1]).toBe('')
    expect(lines[2]).toBe('there')
  })
})
