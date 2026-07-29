import { useCallback } from 'react'
import { resolve } from 'node:path'
import type { Notebook } from 'doughnut-api'
import { downloadNotebookExportZip } from '../../backendApi/doughnutBackendClient.js'
import { writeNotebookExport } from '../../sync/writeNotebookExport.js'
import { AsyncAssistantFetchStage } from '../gmail/AsyncAssistantFetchStage.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
  InteractiveSlashCommandStageProps,
} from '../interactiveSlashCommand.js'

const exportDoc: CommandDoc = {
  name: '/export',
  usage: '/export <destination directory>',
  description:
    'Write the active notebook into a directory as Markdown, ready for Obsidian or any Markdown tool. Creates a subdirectory named after the notebook, so several notebooks can be exported side by side. Files of the same name are overwritten; anything else already there is left alone.',
}

export function exportSlashCommandFor(
  notebook: Notebook
): InteractiveSlashCommand {
  function ExportStage({
    argument,
    onSettled,
    onAbortWithError,
  }: InteractiveSlashCommandStageProps) {
    const runExport = useCallback(
      (signal: AbortSignal) =>
        writeNotebookExport({
          notebookId: notebook.id,
          destinationDirectory: resolve(process.cwd(), (argument ?? '').trim()),
          exportNotebookAsZip: downloadNotebookExportZip,
          signal,
        }),
      [argument]
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

  return {
    literal: '/export',
    doc: exportDoc,
    argument: { name: 'destination directory', optional: false },
    stageComponent: ExportStage,
  }
}
