import { useCallback } from 'react'
import type { Notebook } from 'doughnut-api'
import { downloadNotebookExportZip } from '../../backendApi/doughnutBackendClient.js'
import { applyPull } from '../../sync/applyPull.js'
import { previewPull } from '../../sync/previewPull.js'
import { parseSyncArgument } from '../../sync/syncArgument.js'
import { AsyncAssistantFetchStage } from '../gmail/AsyncAssistantFetchStage.js'
import { UsageErrorStage } from '../UsageErrorStage.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
  InteractiveSlashCommandSettleProps,
  InteractiveSlashCommandStageProps,
} from '../interactiveSlashCommand.js'

const syncDoc: CommandDoc = {
  name: '/sync',
  usage: '/sync [--dry-run] <workspace path>',
  description:
    'Pull remote note changes into a local Markdown workspace, or preview them with --dry-run. Creates, updates, and moves Markdown notes to match the remote export; reserved and unsafe paths are rejected.',
}

type SyncRunStageProps = InteractiveSlashCommandSettleProps & {
  readonly notebookId: number
  readonly workspacePath: string
  readonly dryRun: boolean
}

/** Run the pull, or the preview of it, against an argument already read. */
function SyncRunStage({
  notebookId,
  workspacePath,
  dryRun,
  onSettled,
  onAbortWithError,
}: SyncRunStageProps) {
  const runSync = useCallback(
    (signal: AbortSignal) => {
      const request = {
        notebookId,
        workspacePath,
        exportNotebookAsZip: downloadNotebookExportZip,
        signal,
      }
      return dryRun ? previewPull(request) : applyPull(request)
    },
    [notebookId, workspacePath, dryRun]
  )

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

export function syncSlashCommandFor(
  notebook: Notebook
): InteractiveSlashCommand {
  function SyncStage({
    argument,
    onSettled,
    onAbortWithError,
  }: InteractiveSlashCommandStageProps) {
    const parsed = parseSyncArgument(argument)

    return parsed.error === undefined ? (
      <SyncRunStage
        notebookId={notebook.id}
        workspacePath={parsed.workspacePath}
        dryRun={parsed.dryRun}
        onSettled={onSettled}
        onAbortWithError={onAbortWithError}
      />
    ) : (
      <UsageErrorStage
        message={parsed.error}
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
