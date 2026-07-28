import { useCallback } from 'react'
import type { Notebook } from 'doughnut-api'
import { downloadNotebookExportZip } from '../../backendApi/doughnutBackendClient.js'
import { previewPull } from '../../sync/previewPull.js'
import { parseSyncArgument } from '../../sync/syncArgument.js'
import { AsyncAssistantFetchStage } from '../gmail/AsyncAssistantFetchStage.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
  InteractiveSlashCommandStageProps,
} from '../interactiveSlashCommand.js'

const syncDoc: CommandDoc = {
  name: '/sync',
  usage: '/sync --dry-run <workspace path>',
  description:
    'Preview what pulling the active notebook would change in a local Markdown workspace. Reports a unified diff per changed note and writes nothing. Only --dry-run is available; pulling is not implemented yet.',
}

export function syncSlashCommandFor(
  notebook: Notebook
): InteractiveSlashCommand {
  function SyncStage({
    argument,
    onSettled,
    onAbortWithError,
  }: InteractiveSlashCommandStageProps) {
    const runPreview = useCallback(
      (signal: AbortSignal) => {
        const parsed = parseSyncArgument(argument)
        if (parsed.error !== undefined) {
          return Promise.reject(new Error(parsed.error))
        }
        return previewPull({
          notebookId: notebook.id,
          workspacePath: parsed.workspacePath,
          exportNotebookAsZip: downloadNotebookExportZip,
          signal,
        })
      },
      [argument]
    )

    return (
      <AsyncAssistantFetchStage
        spinnerLabel="Comparing the workspace with the notebook…"
        runAssistantMessage={runPreview}
        onSettled={onSettled}
        onAbortWithError={onAbortWithError}
      />
    )
  }

  return {
    literal: '/sync',
    doc: syncDoc,
    argument: { name: '--dry-run <workspace path>', optional: false },
    stageComponent: SyncStage,
  }
}
