import { describe, expect, test } from 'vitest'
import { renderDiffReport, renderNoteDiff } from '../src/sync/diffReport.js'

describe('renderNoteDiff', () => {
  test('names the workspace as the side an unlabeled diff reads from', () => {
    expect(renderNoteDiff('less.md', 'Hello', 'Hello world!')).toBe(
      [
        'less.md',
        '  --- workspace',
        '  +++ Doughnut',
        '  - Hello',
        '  + Hello world!',
        '',
      ].join('\n')
    )
  })

  test('reads a pull from the workspace to Doughnut', () => {
    expect(renderNoteDiff('less.md', 'Hello', 'Hello world!', 'pull')).toBe(
      [
        'less.md (pull)',
        '  --- workspace',
        '  +++ Doughnut',
        '  - Hello',
        '  + Hello world!',
        '',
      ].join('\n')
    )
  })

  test('reads a push from Doughnut to the workspace', () => {
    expect(
      renderNoteDiff('less.md', 'Hello from Obsidian', 'Hello', 'push')
    ).toBe(
      [
        'less.md (push)',
        '  --- Doughnut',
        '  +++ workspace',
        '  - Hello',
        '  + Hello from Obsidian',
        '',
      ].join('\n')
    )
  })

  test('labels a conflict in uppercase and reads it like a pull', () => {
    expect(
      renderNoteDiff(
        'less.md',
        'Hello from Obsidian',
        'Hello world!',
        'conflict'
      )
    ).toBe(
      [
        'less.md (CONFLICT)',
        '  --- workspace',
        '  +++ Doughnut',
        '  - Hello from Obsidian',
        '  + Hello world!',
        '',
      ].join('\n')
    )
  })
})

describe('renderDiffReport', () => {
  test('reports nothing when no note is reported', () => {
    expect(renderDiffReport([], 'No changes to pull.')).toBe(
      'No changes to pull.'
    )
  })

  test('counts a single changed note', () => {
    expect(renderDiffReport([{ diff: 'less.md\n' }], 'nothing')).toBe(
      'less.md\n\n1 note would change.'
    )
  })

  test('counts changed notes in the plural', () => {
    expect(renderDiffReport([{ diff: 'a' }, { diff: 'b' }], 'nothing')).toBe(
      'a\nb\n2 notes would change.'
    )
  })

  test('counts a single conflict on its own', () => {
    expect(
      renderDiffReport([{ diff: 'a', status: 'conflict' }], 'nothing')
    ).toBe('a\n1 conflict.')
  })

  test('counts conflicts in the plural', () => {
    expect(
      renderDiffReport(
        [
          { diff: 'a', status: 'conflict' },
          { diff: 'b', status: 'conflict' },
        ],
        'nothing'
      )
    ).toBe('a\nb\n2 conflicts.')
  })

  test('counts conflicts apart from the notes that would change', () => {
    expect(
      renderDiffReport(
        [
          { diff: 'a', status: 'push' },
          { diff: 'b', status: 'pull' },
          { diff: 'c', status: 'conflict' },
        ],
        'nothing'
      )
    ).toBe('a\nb\nc\n2 notes would change. 1 conflict.')
  })
})
