import { notebookList } from './pageObjects/NotebookList'
import { assumeNotePage } from './pageObjects/notePage'
import { noteSidebar } from './pageObjects/noteSidebar'

export const BAZAAR_NOTE_PATH_ROOT = 'Bazaar'

/** Requires the notebook catalog UI (My notebooks or Bazaar) to already be visible. */
export function navigateAlongNotebookCatalogPath(segments: string[]) {
  if (segments.length === 0) {
    return
  }
  const [notebookName, ...titles] = segments
  const notebook = notebookList().navigateToNotebook(notebookName!)
  if (titles.length === 0) {
    return notebook
  }

  const sidebar = noteSidebar()

  if (titles.length === 1) {
    sidebar.navigateToNote(titles[0]!)
    return assumeNotePage(titles[0]!)
  }

  const folderLabels = titles.slice(0, -1)
  const leafNoteTitle = titles[titles.length - 1]!

  for (const folderLabel of folderLabels) {
    sidebar.navigateExpandFolder(folderLabel)
  }
  sidebar.navigateToNote(leafNoteTitle)
  return assumeNotePage(leafNoteTitle)
}
