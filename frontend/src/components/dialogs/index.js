import NoteEditDialog from "./NoteEditDialog.vue";
import NoteTranslationEditDialog from "./NoteTranslationEditDialog.vue";
import Languages from "../../models/languages"

const editNote = (popups, noteId, language) => {
    if(language === Languages.ID) {
      return popups.dialog(NoteTranslationEditDialog, { noteId });
    }

      return popups.dialog(NoteEditDialog, { noteId });
    }

export { editNote, Languages }