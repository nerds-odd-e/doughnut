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

  test('offers export inside the notebook context', () => {
    const literals = notebookStageSlashCommandsFor(aNotebook()).map(
      (command) => command.literal
    )

    expect(literals).toContain('/export')
  })

  test('documents where export writes and what it overwrites', () => {
    const exportCommand = notebookStageSlashCommandsFor(aNotebook()).find(
      (command) => command.literal === '/export'
    )

    expect(exportCommand?.doc.usage).toBe('/export <destination directory>')
    expect(exportCommand?.doc.description).toContain('subdirectory')
    expect(exportCommand?.doc.description).toContain('overwritten')
    expect(exportCommand?.argument?.optional).toBe(false)
  })

  test('offers push inside the notebook context', () => {
    const literals = notebookStageSlashCommandsFor(aNotebook()).map(
      (command) => command.literal
    )

    expect(literals).toContain('/push')
  })

  test('documents dry-run push usage', () => {
    const push = notebookStageSlashCommandsFor(aNotebook()).find(
      (command) => command.literal === '/push'
    )

    expect(push?.doc.usage).toBe('/push --dry-run <workspace path>')
    expect(push?.doc.description).toContain('Preview')
    expect(push?.argument?.optional).toBe(false)
  })
})
