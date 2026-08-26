import makeMe from 'donut-test-fixtures/makeMe'
import { describe, expect, test } from 'vitest'
import { notebookStageSlashCommandsFor } from '../src/commands/notebook/notebookStageSlashCommands.js'

const aNotebook = () =>
  makeMe.aNotebook
    .withSeedNote(makeMe.aNote.title('Ben Notebook').please())
    .do()

describe('notebookStageSlashCommandsFor', () => {
  test('offers attach and exit inside the notebook context', () => {
    const literals = notebookStageSlashCommandsFor(aNotebook()).map(
      (command) => command.literal
    )

    expect(literals).toEqual(['/attach', '/exit'])
  })
})
