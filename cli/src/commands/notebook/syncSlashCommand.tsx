import { useCallback } from 'react'
import type { Notebook } from 'doughnut-api'
import { downloadNotebookExportZip } from '../../backendApi/doughnutBackendClient.js'
import { applyPull } from '../../sync/applyPull.js'
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
  usage: '/sync [--dry-run] <workspace path>',
  description:
    'Pull remote note changes into a local Markdown workspace, or preview them with --dry-run. Only updates files that already exist locally and match an exported note path.',
}

export function syncSlashCommandFor(
  notebook: Notebook
): InteractiveSlashCommand {
  function SyncStage({
    argument,
    onSettled,
    onAbortWithError,
  }: InteractiveSlashCommandStageProps) {
    const runSync = useCallback(
      (signal: AbortSignal) => {
        const parsed = parseSyncArgument(argument)
        if (parsed.error !== undefined) {
          return Promise.reject(new Error(parsed.error))
        }
        const request = {
          notebookId: notebook.id,
          workspacePath: parsed.workspacePath,
          exportNotebookAsZip: downloadNotebookExportZip,
          signal,
        }
        return parsed.dryRun ? previewPull(request) : applyPull(request)
      },
      [argument]
    )

    const parsed = parseSyncArgument(argument)
    const dryRun = parsed.error === undefined && parsed.dryRun

    return (
      <AsyncAssistantFetchStage
        spinnerLabel={
          dryRun
            ? 'Comparing the workspace with the notebook…'
            : 'Pulling remote changes into the workspace…'
        }
        runAssistantMessage={runSync}
        onSettled={onSettled}
        onAbortWithError={onAbortWithError}
      />
    )
  }

  return {
    literal: '/sync',
    doc: syncDoc,
    argument: { name: '[--dry-run] <workspace path>', optional: false },
    stageComponent: SyncStage,
  }
}
