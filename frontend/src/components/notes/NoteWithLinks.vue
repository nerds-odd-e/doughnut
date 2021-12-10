<template>
  <NoteShell
    class="note-body"
    v-bind="{ id: note.id, updatedAt: note.noteContent?.updatedAt, language, isEditingTitle }"
  >
    <NoteFrameOfLinks v-bind="{ links: note.links }">
      <h2 id="title-id" role="title" class="note-title" style="display: inline-block;" @click="onTitleClick" v-if="!isEditingTitle">{{ translatedNote.title }}</h2>
      <span 
        role="outdated-tag" 
        class="outdated-label" 
        v-if="translatedNote.isTranslationOutdatedIDN && !isEditingTitle"
        >
          Outdated translation
      </span>
      <TextInput id="title-form-id" role="title-input" scopeName="note" v-model="translatedNote.title" :autofocus="true" @blur="onBlurTextField" v-if="isEditingTitle"/>
      <p
        style="color: red"
        role="title-fallback"
        v-if="translatedNote.translationNoteAvailable"
      >
        No translation available
      </p>
      <NoteContent v-bind="{ note, language, isInPlaceEditEnabled, isEditingTitle }"/>
    </NoteFrameOfLinks>
  </NoteShell>
</template>

<script>
import TextInput from "../form/TextInput.vue";
import NoteFrameOfLinks from "../links/NoteFrameOfLinks.vue";
import NoteShell from "./NoteShell.vue";
import NoteContent from "./NoteContent.vue";
import Languages, { TranslatedNoteWrapper } from "../../models/languages";
import { restPatchMultiplePartForm } from "../../restful/restful";

export default {
  name: "NoteWithLinks",
  props: {
    note: Object,
    language: String,
    isInPlaceEditEnabled: Boolean,
    isEditingTitle: Boolean
  },
  components: {
    NoteFrameOfLinks,
    NoteShell,
    NoteContent,
    TextInput
  },
  data() {
    return {
      isEditingTitle: false,
    };
  },
  computed: {
    translatedNote(){
      return new TranslatedNoteWrapper(this.note, this.language);
    },
  },
  methods: {
    onTitleClick() {
      if (this.isInPlaceEditEnabled) {
        this.isEditingTitle = true;
      }
    },
    onBlurTextField(input) {
      const resolvedLanguage = this.language ?? Languages.EN;

      if (resolvedLanguage === Languages.EN) {
        this.note.title = input.target.value;
        this.note.titleIDN = this.note.noteContent.titleIDN;
        this.note.noteContent.title = input.target.value;

      } else if (resolvedLanguage === Languages.ID) {
        this.note.title = this.note.noteContent.title;
        this.note.titleIDN = input.target.value;
        this.note.noteContent.titleIDN = input.target.value;
      }

      // Need to update description to make sure we're calling API with correct value.
      this.note.description = this.note.noteContent.description;
      this.note.descriptionIDN = this.note.noteContent.descriptionIDN;

      this.isEditingTitle = false;
      restPatchMultiplePartForm(
        `/api/notes/${this.note.id}`,
        this.note,
        (r) => (this.loading = r)
      )
        .then((res) => {
          this.$store.commit("loadNotes", [res]);
          this.$emit("done");
        })
        .catch((res) => (this.formErrors = res));
    }
  },
};
</script>

<style scoped>
.note-body {
  padding-left: 10px;
  padding-right: 10px;
  border-radius: 10px;
  border-style: solid;
  border-top-width: 3px;
  border-bottom-width: 1px;
  border-right-width: 3px;
  border-left-width: 1px;
}

.note-title {
  margin-top: 0px;
  padding-top: 10px;
  color: black;
}

.outdated-label {
  display: inline-block;
  height: 100%;
  vertical-align: middle;
  margin-left: 20px;
  padding-bottom: 10px;
  color: red;
}
</style>
