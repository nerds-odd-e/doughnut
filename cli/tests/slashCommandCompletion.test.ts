import makeMe from 'doughnut-test-fixtures/makeMe'
import { describe, expect, test } from 'vitest'
import type { InteractiveSlashCommand } from '../src/commands/interactiveSlashCommand.js'
import { notebookStageSlashCommandsFor } from '../src/commands/notebook/notebookStageSlashCommands.js'
import {
  getSlashTabCompletion,
  slashGuidanceForInk,
  slashGuidanceUsageColumnWidth,
} from '../src/mainInteractivePrompt/slashCommandCompletion.js'

const aNotebook = () =>
  makeMe.aNotebook
    .withSeedNote(makeMe.aNote.title('Ben Notebook').please())
    .do()

describe('getSlashTabCompletion', () => {
  test('/ex stops short of a full command: /export and /exit share the prefix', () => {
    const commands = notebookStageSlashCommandsFor(aNotebook())

    expect(getSlashTabCompletion('/ex', commands)).toEqual({
      completed: '/ex',
      count: 2,
    })
  })
})

describe('slash guidance usage column cap', () => {
  test('column width ignores usages wider than cap', () => {
    expect(
      slashGuidanceUsageColumnWidth(
        [{ usage: '/short' }, { usage: '/very-long-command-name <arg>' }],
        26
      )
    ).toBe(6)
  })

  test('column width is capped', () => {
    expect(
      slashGuidanceUsageColumnWidth(
        [{ usage: '/a' }, { usage: '/medium-length' }],
        26
      )
    ).toBe(14)
  })
})

describe('slash guidance filtering', () => {
  const command = (
    literal: string,
    usage: string
  ): InteractiveSlashCommand => ({
    literal,
    doc: { name: literal, usage, description: 'does something' },
    run: () => ({ assistantMessage: '' }),
  })

  const listed = (draft: string, commands: InteractiveSlashCommand[]) => {
    const guidance = slashGuidanceForInk(draft, commands)
    return guidance.show === 'list'
      ? guidance.rows.map((row) => row.completionLine)
      : guidance.show
  }

  test('offers a command the draft appears anywhere in the name of', () => {
    expect(
      listed('/call', [
        command('/recall', '/recall'),
        command('/exit', '/exit'),
      ])
    ).toEqual(['/recall'])
  })

  test('does not offer a command whose argument name contains the draft', () => {
    expect(
      listed('/re', [
        command('/recall', '/recall'),
        command('/lint', '/lint <workspace directory>'),
      ])
    ).toEqual(['/recall'])
  })
})
