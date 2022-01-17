import NoteEditDialog from "./NoteEditDialog.vue";

const editNote = (popups, noteId) => popups.dialog(NoteEditDialog, { noteId })

const version = 0

export { editNote, version }