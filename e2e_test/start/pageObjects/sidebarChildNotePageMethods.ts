import { noteSidebar } from './noteSidebar'

export const sidebarChildNotePageMethods = () => ({
  addingChildNoteButton() {
    return noteSidebar().addingChildNoteButton()
  },
  addingChildNote() {
    return noteSidebar().addingChildNote()
  },
  addingNewNoteFromToolbar() {
    return noteSidebar().addingNewNoteFromToolbar()
  },
  addingNewFolderFromToolbar() {
    return noteSidebar().addingNewFolderFromToolbar()
  },
})
