import assert from 'node:assert/strict'
import './interactive/interactiveTestMocks.js'
import { afterEach, beforeEach, describe, test, type vi } from 'vitest'
import { filterCommandsByPrefix, interactiveDocs } from '../src/help.js'
import {
  endTTYSession,
  pressKey,
  tick,
  ttyOutput,
  ttySessionWithSpies,
  typeString,
  type TTYStdin,
} from './interactive/interactiveTestHelpers.js'

const SLASH_PICKER_ARROWS_HINT =
  'When "/" has opened the slash-command list, ArrowDown and ArrowUp must move which command is highlighted (reverse video \\x1b[7m), like token/MCQ pickers. ' +
  'If this fails, those keys are probably still wired to caret jump (start/end of line) or command history instead of the suggestion highlight.'

function lastStdoutLineContaining(
  output: string,
  needle: string
): string | undefined {
  let found: string | undefined
  for (const line of output.split('\n')) {
    if (line.includes(needle)) found = line
  }
  return found
}

describe('TTY: slash-command picker arrow keys (regression)', () => {
  let writeSpy: ReturnType<typeof vi.spyOn>
  let stdin: TTYStdin

  beforeEach(async () => {
    ;({ stdin, writeSpy } = await ttySessionWithSpies())
  })

  afterEach(() => {
    endTTYSession(stdin)
  })

  test('ArrowDown then ArrowUp move the highlighted command, not only the caret', async () => {
    const filtered = filterCommandsByPrefix(interactiveDocs, '/')
    assert.ok(
      filtered.length >= 2,
      'This regression test needs at least two interactive commands so "/" shows a real list.'
    )
    const firstUsage = filtered[0]!.usage
    const secondUsage = filtered[1]!.usage
    const needleFirst = `  ${firstUsage}`
    const needleSecond = `  ${secondUsage}`

    writeSpy.mockClear()
    typeString(stdin, '/')
    await tick()
    pressKey(stdin, 'down')
    await tick()

    const afterDown = ttyOutput(writeSpy)
    const secondLine = lastStdoutLineContaining(afterDown, needleSecond)
    assert.ok(
      secondLine?.includes('\x1b[7m'),
      `${SLASH_PICKER_ARROWS_HINT}\nAfter ArrowDown, the "${secondUsage}" row should be reverse-video highlighted.`
    )
    const firstLineAfterDown = lastStdoutLineContaining(afterDown, needleFirst)
    assert.ok(
      firstLineAfterDown && !firstLineAfterDown.includes('\x1b[7m'),
      `${SLASH_PICKER_ARROWS_HINT}\nAfter ArrowDown, the "${firstUsage}" row should not stay highlighted.`
    )

    pressKey(stdin, 'up')
    await tick()

    const afterUp = ttyOutput(writeSpy)
    const firstLineAfterUp = lastStdoutLineContaining(afterUp, needleFirst)
    assert.ok(
      firstLineAfterUp?.includes('\x1b[7m'),
      `${SLASH_PICKER_ARROWS_HINT}\nAfter ArrowUp from the second command, "${firstUsage}" should be highlighted again.`
    )
    const secondLineAfterUp = lastStdoutLineContaining(afterUp, needleSecond)
    assert.ok(
      secondLineAfterUp && !secondLineAfterUp.includes('\x1b[7m'),
      `${SLASH_PICKER_ARROWS_HINT}\nAfter ArrowUp, "${secondUsage}" should no longer be highlighted.`
    )
  })
})
