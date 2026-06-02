export type UseNotebookInkWaitHelpers = {
  waitForLastFrameToInclude: (
    pattern: string | RegExp,
    maxTicks?: number
  ) => Promise<void>
  waitUntilLastFrame: (
    predicate: (stripped: string) => boolean,
    maxTicks?: number
  ) => Promise<void>
}

export async function openTopMathsNotebook(
  stdin: { write(data: string): void },
  ink: UseNotebookInkWaitHelpers
): Promise<void> {
  stdin.write('/use Top Maths\r')
  await ink.waitForLastFrameToInclude('Active notebook: Top Maths')
}

export async function waitNotebookSlashGuidance(
  ink: UseNotebookInkWaitHelpers
): Promise<void> {
  await ink.waitUntilLastFrame(
    (f) =>
      f.includes('/attach <path to .pdf or .epub>') &&
      f.includes('Attach a book file to the active notebook') &&
      f.includes('(POST attach-book)') &&
      f.includes('/exit, exit') &&
      f.includes('Leave notebook context')
  )
}
