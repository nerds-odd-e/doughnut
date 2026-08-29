import type NotePath from '../support/NotePath'
import { navigationActions } from './actions/navigationActions'
import { navigateToBazaar } from './pageObjects/bazaarPage'
import { notebookList } from './pageObjects/NotebookList'
import { assumeNotePage } from './pageObjects/notePage'
import { noteSidebar } from './pageObjects/noteSidebar'

const BAZAAR_NOTE_PATH_ROOT = 'Bazaar'

/** Owned paths identity-jump to the leaf note. Bazaar-rooted paths walk the bazaar catalog. */
export function navigateToNoteFromPath(notePath: NotePath) {
  const segments = notePath.path
  if (segments.length === 0) {
    return
  }
  if (segments[0] === BAZAAR_NOTE_PATH_ROOT) {
    navigateToBazaarNoteFromPath(segments)
    return
  }
  jumpToOwnedNoteFromPath(segments)
}

function navigateToBazaarNoteFromPath(segments: string[]) {
  navigateToBazaar()
  const catalogSegments = segments.slice(1)
  if (catalogSegments.length === 0) {
    return
  }
  navigateAlongNotebookCatalogPath(catalogSegments)
}

function jumpToOwnedNoteFromPath(segments: string[]) {
  navigationActions.jumpToNotePage(segments[segments.length - 1]!)
}

function navigateAlongSidebarToNote(titles: string[]) {
  const sidebar = noteSidebar()

  if (titles.length === 1) {
    sidebar.navigateToNote(titles[0]!)
    return assumeNotePage(titles[0]!)
  }

  const folderLabels = titles.slice(0, -1)
  const leafNoteTitle = titles[titles.length - 1]!

  for (const segment of folderLabels) {
    sidebar.expand(segment)
  }
  sidebar.navigateToNote(leafNoteTitle)
  return assumeNotePage(leafNoteTitle)
}

/** Requires the notebook catalog UI (Notebooks or Bazaar) to already be visible. */
export function navigateAlongNotebookCatalogPath(segments: string[]) {
  if (segments.length === 0) {
    return
  }
  const [notebookName, ...titles] = segments
  const notebook = notebookList().navigateToNotebook(notebookName!)
  if (titles.length === 0) {
    return notebook
  }

  return navigateAlongSidebarToNote(titles)
}

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
    sidebar.expand(segment)
  }
}
