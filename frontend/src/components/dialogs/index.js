import NoteEditDialog from "./NoteEditDialog.vue";

const editNote = (popups, noteId) => {
      return popups.dialog(NoteEditDialog, { noteId });
    }

export { editNote }