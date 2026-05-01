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

  for (const segment of folderLabels) {
    sidebar.navigateStructuralIntermediate(segment)
  }
  sidebar.navigateToNote(leafNoteTitle)
  return assumeNotePage(leafNoteTitle)
}

/**
 * Opens the notebook and walks the sidebar by structural segments only; the last path
 * segment is treated as a folder (expanded / structural), not opened as a note view.
 * Use {@link navigateAlongNotebookCatalogPath} when the last segment is a note to open.
 */
export function openFolderAlongNotebookCatalogPath(segments: string[]) {
  if (segments.length < 2) {
    throw new Error(
      'openFolder path needs at least a notebook name and one folder segment'
    )
  }
  const [notebookName, ...pathInNotebook] = segments
  notebookList().navigateToNotebook(notebookName!)
  const sidebar = noteSidebar()
  for (const segment of pathInNotebook) {
    sidebar.expandStructuralIntermediateFolderOnly(segment)
  }
}
