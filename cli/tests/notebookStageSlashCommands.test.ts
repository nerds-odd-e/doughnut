import makeMe from 'doughnut-test-fixtures/makeMe'
import { describe, expect, test } from 'vitest'
import { notebookStageSlashCommandsFor } from '../src/commands/notebook/notebookStageSlashCommands.js'

const aNotebook = () =>
  makeMe.aNotebook
    .withSeedNote(makeMe.aNote.title('Ben Notebook').please())
    .do()

describe('notebookStageSlashCommandsFor', () => {
  test('offers sync inside the notebook context', () => {
    const literals = notebookStageSlashCommandsFor(aNotebook()).map(
      (command) => command.literal
    )

    expect(literals).toContain('/sync')
  })

  test('documents pull and dry-run usage', () => {
    const sync = notebookStageSlashCommandsFor(aNotebook()).find(
      (command) => command.literal === '/sync'
    )

    expect(sync?.doc.usage).toBe('/sync [--dry-run] <workspace path>')
    expect(sync?.doc.description).toContain('--dry-run')
    expect(sync?.doc.description).toContain('Pull')
  })

  test('requires an argument', () => {
    const sync = notebookStageSlashCommandsFor(aNotebook()).find(
      (command) => command.literal === '/sync'
    )

    expect(sync?.argument?.optional).toBe(false)
  })
})
