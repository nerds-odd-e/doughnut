import { notebookList } from './pageObjects/NotebookList'
import { assumeNotePage } from './pageObjects/notePage'
import { noteSidebar } from './pageObjects/noteSidebar'

export const BAZAAR_NOTE_PATH_ROOT = 'Bazaar'

/** Requires the notebook catalog UI (My notebooks or Bazaar) to already be visible. */
export function navigateAlongNotebookCatalogPath(segments: string[]) {
  if (segments.length === 0) {
    return
  }
  const [notebookName, ...noteTitles] = segments
  const notebook = notebookList().navigateToNotebook(notebookName!)
  if (noteTitles.length === 0) {
    return notebook
  }
  noteTitles.forEach((noteTitle) => {
    noteSidebar().navigateToNote(noteTitle)
  })
  const leafTitle = noteTitles[noteTitles.length - 1]!
  return assumeNotePage(leafTitle)
}
