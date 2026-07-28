import { describe, expect, test } from 'vitest'
import { diffLines } from '../src/sync/unifiedDiff.js'

const render = (before: string, after: string) =>
  diffLines(before, after)
    .map((hunk) =>
      [
        hunk.header === undefined ? undefined : `@@ line ${hunk.header} @@`,
        ...hunk.lines.map(({ kind, text }) =>
          kind === 'context'
            ? `  ${text}`
            : `${kind === 'removed' ? '-' : '+'} ${text}`
        ),
      ]
        .filter((line) => line !== undefined)
        .join('\n')
    )
    .join('\n')

describe('diffLines', () => {
  test('reports no hunks when both sides are identical', () => {
    expect(diffLines('Hello', 'Hello')).toEqual([])
  })

  test('reports a one line replacement', () => {
    expect(render('Hello', 'Hello world!')).toBe(
      ['- Hello', '+ Hello world!'].join('\n')
    )
  })

  test('keeps three unchanged lines around a change', () => {
    const before = [
      'Sprint planning',
      'Daily standup',
      'Two week sprint',
      'Retrospective',
      'Demo',
    ]
    const after = [...before]
    after[2] = 'Three week sprint'

    expect(render(before.join('\n'), after.join('\n'))).toBe(
      [
        '  Sprint planning',
        '  Daily standup',
        '- Two week sprint',
        '+ Three week sprint',
        '  Retrospective',
        '  Demo',
      ].join('\n')
    )
  })

  test('drops context beyond three lines from the change', () => {
    const before = [
      'one',
      'two',
      'three',
      'four',
      'CHANGED',
      'six',
      'seven',
      'eight',
      'nine',
    ]
    const after = [...before]
    after[4] = 'changed'

    expect(render(before.join('\n'), after.join('\n'))).toBe(
      [
        '  two',
        '  three',
        '  four',
        '- CHANGED',
        '+ changed',
        '  six',
        '  seven',
        '  eight',
      ].join('\n')
    )
  })

  test('reports an added line with no matching removal', () => {
    expect(
      render(
        'Sprint planning\nRetrospective',
        'Sprint planning\nDaily standup\nRetrospective'
      )
    ).toBe(
      ['  Sprint planning', '+ Daily standup', '  Retrospective'].join('\n')
    )
  })

  test('reports a removed line with no matching addition', () => {
    expect(
      render(
        'Sprint planning\nDaily standup\nRetrospective',
        'Sprint planning\nRetrospective'
      )
    ).toBe(
      ['  Sprint planning', '- Daily standup', '  Retrospective'].join('\n')
    )
  })

  test('shows no context before a change on the first line', () => {
    expect(
      render(
        'Sprint planning\nDaily standup\nRetrospective',
        'Sprint planning meeting\nDaily standup\nRetrospective'
      )
    ).toBe(
      [
        '- Sprint planning',
        '+ Sprint planning meeting',
        '  Daily standup',
        '  Retrospective',
      ].join('\n')
    )
  })

  test('shows no context after a change on the last line', () => {
    expect(
      render(
        'Sprint planning\nDaily standup\nRetrospective',
        'Sprint planning\nDaily standup\nRetrospective and demo'
      )
    ).toBe(
      [
        '  Sprint planning',
        '  Daily standup',
        '- Retrospective',
        '+ Retrospective and demo',
      ].join('\n')
    )
  })

  test('splits changes separated by more than twice the context into two hunks', () => {
    const before = [
      'Sprint planning',
      'Two week sprint',
      'Daily standup',
      'Backlog refinement',
      'Story mapping',
      'Estimation',
      'Definition of done',
      'Working agreement',
      'Team charter',
      'Retrospective',
      'Demo',
    ]
    const after = [...before]
    after[1] = 'Three week sprint'
    after[9] = 'Retrospective and demo'

    expect(render(before.join('\n'), after.join('\n'))).toBe(
      [
        '@@ line 1 @@',
        '  Sprint planning',
        '- Two week sprint',
        '+ Three week sprint',
        '  Daily standup',
        '  Backlog refinement',
        '  Story mapping',
        '@@ line 7 @@',
        '  Definition of done',
        '  Working agreement',
        '  Team charter',
        '- Retrospective',
        '+ Retrospective and demo',
        '  Demo',
      ].join('\n')
    )
  })

  test('joins changes whose context would touch into one hunk', () => {
    // Six unchanged lines between the changes: the context of each reaches the
    // other, so one hunk covers both and needs no heading.
    const before = [
      'one',
      'CHANGED',
      'a',
      'b',
      'c',
      'd',
      'e',
      'f',
      'CHANGED TOO',
      'ten',
    ]
    const after = [...before]
    after[1] = 'changed'
    after[8] = 'changed too'

    expect(render(before.join('\n'), after.join('\n'))).toBe(
      [
        '  one',
        '- CHANGED',
        '+ changed',
        '  a',
        '  b',
        '  c',
        '  d',
        '  e',
        '  f',
        '- CHANGED TOO',
        '+ changed too',
        '  ten',
      ].join('\n')
    )
  })

  test('joins adjacent changes into one hunk', () => {
    const before = ['one', 'CHANGED', 'three', 'four', 'CHANGED TOO', 'six']
    const after = [...before]
    after[1] = 'changed'
    after[4] = 'changed too'

    expect(render(before.join('\n'), after.join('\n'))).toBe(
      [
        '  one',
        '- CHANGED',
        '+ changed',
        '  three',
        '  four',
        '- CHANGED TOO',
        '+ changed too',
        '  six',
      ].join('\n')
    )
  })

  test('treats a blank line as content', () => {
    expect(
      render(
        'Sprint planning\n\nRetrospective',
        'Sprint planning\nDaily standup\nRetrospective'
      )
    ).toBe(
      ['  Sprint planning', '- ', '+ Daily standup', '  Retrospective'].join(
        '\n'
      )
    )
  })

  test('reports emptied content as a removal', () => {
    expect(render('Hello', '')).toBe('- Hello')
  })

  test('compares markdown markup as raw text', () => {
    expect(
      render(
        '**Put** to sleep is _sedation_',
        '**Put** to sleep is **sedation**'
      )
    ).toBe(
      [
        '- **Put** to sleep is _sedation_',
        '+ **Put** to sleep is **sedation**',
      ].join('\n')
    )
  })
})
