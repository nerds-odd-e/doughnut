import { render } from 'ink-testing-library'
import makeMe from 'doughnut-test-fixtures/makeMe'
import { describe, expect, test } from 'vitest'
import { exportSlashCommandFor } from '../src/commands/notebook/exportSlashCommand.js'
import { waitForFrames } from './inkTestHelpers.js'

const aNotebook = () =>
  makeMe.aNotebook
    .withSeedNote(makeMe.aNote.title('Ben Notebook').please())
    .do()

describe('exportSlashCommandFor', () => {
  test('a missing destination reports the error without ever showing the export spinner', async () => {
    const Stage = exportSlashCommandFor(aNotebook()).stageComponent
    let aborted: string | null = null
    const { frames } = render(
      <Stage
        argument="/no/such/directory-for-export-slash-command-test"
        onSettled={() => {
          throw new Error('should not settle successfully')
        }}
        onAbortWithError={(message) => {
          aborted = message
        }}
      />
    )

    await waitForFrames(
      () => aborted ?? '',
      (s) => s.startsWith('No directory at')
    )

    expect(aborted).toContain('No directory at')
    expect(frames.join('\n')).not.toContain('Exporting the notebook')
  })
})
