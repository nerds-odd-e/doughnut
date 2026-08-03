import { useCallback } from 'react'
import type { Notebook } from 'doughnut-api'
import { downloadNotebookExportZip } from '../../backendApi/doughnutBackendClient.js'
import { previewPush } from '../../sync/previewPush.js'
import { parsePushArgument } from '../../sync/pushArgument.js'
import { AsyncAssistantFetchStage } from '../gmail/AsyncAssistantFetchStage.js'
import { UsageErrorStage } from '../UsageErrorStage.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
  InteractiveSlashCommandSettleProps,
  InteractiveSlashCommandStageProps,
} from '../interactiveSlashCommand.js'

const pushDoc: CommandDoc = {
  name: '/push',
  usage: '/push --dry-run <workspace path>',
  description:
    'Preview what pushing the workspace would change in Doughnut. Requires --dry-run.',
}

type PushRunStageProps = InteractiveSlashCommandSettleProps & {
  readonly notebookId: number
  readonly workspacePath: string
}

/** Run the preview of a push against an argument already read. */
function PushRunStage({
  notebookId,
  workspacePath,
  onSettled,
  onAbortWithError,
}: PushRunStageProps) {
  const runPreviewPush = useCallback(
    (signal: AbortSignal) =>
      previewPush({
        notebookId,
        workspacePath,
        exportNotebookAsZip: downloadNotebookExportZip,
        signal,
      }),
    [notebookId, workspacePath]
  )

  return (
    <AsyncAssistantFetchStage
      spinnerLabel="Comparing the workspace with the notebook…"
      runAssistantMessage={runPreviewPush}
      onSettled={onSettled}
      onAbortWithError={onAbortWithError}
    />
  )
}

export function pushSlashCommandFor(
  notebook: Notebook
): InteractiveSlashCommand {
  function PushStage({
    argument,
    onSettled,
    onAbortWithError,
  }: InteractiveSlashCommandStageProps) {
    const parsed = parsePushArgument(argument)

    return parsed.error === undefined ? (
      <PushRunStage
        notebookId={notebook.id}
        workspacePath={parsed.workspacePath}
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
    literal: '/push',
    doc: pushDoc,
    argument: { name: '--dry-run <workspace path>', optional: false },
    stageComponent: PushStage,
  }
}
