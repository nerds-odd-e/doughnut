import type { InkTestRenderResult } from './inkTestHelpers.js'
import {
  extendInkRenderForInteractiveTests,
  inkCommandLineProbeUndelete,
} from './inkTestHelpers.js'

export type NotebookStageInkHelpers = ReturnType<
  typeof extendInkRenderForInteractiveTests
>

export async function waitNotebookStageActive(
  ink: NotebookStageInkHelpers,
  notebookName: string
): Promise<void> {
  await ink.waitUntilLastFrame((f) =>
    f.includes(`Active notebook: ${notebookName}`)
  )
}

/** Waits for nested notebook shell prompt (last frame), then probes command-line readiness. */
export async function readyNotebookStageRender(
  result: InkTestRenderResult,
  notebookName: string
) {
  const ink = extendInkRenderForInteractiveTests(result)
  await waitNotebookStageActive(ink, notebookName)
  await inkCommandLineProbeUndelete(result, {
    probeChar: '|',
    probeVisible: (f) => f.includes('→ |') || f.includes('> |'),
    probeHidden: (f) =>
      (f.includes('→') && !f.includes('→ |')) ||
      (f.includes('>') && !f.includes('> |')),
  })
  return { ...result, ...ink }
}

export async function waitNotebookPickerVisible(
  ink: NotebookStageInkHelpers
): Promise<void> {
  await ink.waitUntilLastFrame((f) => f.includes('Pick a notebook'))
}
