import { useCallback, useMemo } from 'react'
import type { Notebook } from 'doughnut-api'
import { downloadNotebookExportZip } from '../../backendApi/doughnutBackendClient.js'
import { parseExportDestination } from '../../sync/exportDestination.js'
import { writeNotebookExport } from '../../sync/writeNotebookExport.js'
import { AsyncAssistantFetchStage } from '../gmail/AsyncAssistantFetchStage.js'
import { UsageErrorStage } from '../UsageErrorStage.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
  InteractiveSlashCommandSettleProps,
  InteractiveSlashCommandStageProps,
} from '../interactiveSlashCommand.js'

const exportDoc: CommandDoc = {
  name: '/export',
  usage: '/export <destination directory>',
  description:
    'Write the active notebook into an existing directory as Markdown, ready for Obsidian or any Markdown tool. Creates a subdirectory named after the notebook, so several notebooks can be exported side by side. Files of the same name are overwritten; anything else already there is left alone.',
}

type ExportRunStageProps = InteractiveSlashCommandSettleProps & {
  readonly notebookId: number
  readonly destinationDirectory: string
}

/** Run the export against a destination already parsed and confirmed to exist. */
function ExportRunStage({
  notebookId,
  destinationDirectory,
  onSettled,
  onAbortWithError,
}: ExportRunStageProps) {
  const runExport = useCallback(
    (signal: AbortSignal) =>
      writeNotebookExport({
        notebookId,
        destinationDirectory,
        exportNotebookAsZip: downloadNotebookExportZip,
        signal,
      }),
    [notebookId, destinationDirectory]
  )

  return (
    <AsyncAssistantFetchStage
      spinnerLabel="Exporting the notebook…"
      runAssistantMessage={runExport}
      onSettled={onSettled}
      onAbortWithError={onAbortWithError}
    />
  )
}

export function exportSlashCommandFor(
  notebook: Notebook
): InteractiveSlashCommand {
  function ExportStage({
    argument,
    onSettled,
    onAbortWithError,
  }: InteractiveSlashCommandStageProps) {
    const parsed = useMemo(() => parseExportDestination(argument), [argument])

    return parsed.error === undefined ? (
      <ExportRunStage
        notebookId={notebook.id}
        destinationDirectory={parsed.directory}
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
    literal: '/export',
    doc: exportDoc,
    argument: { name: 'destination directory', optional: false },
    stageComponent: ExportStage,
  }
}
