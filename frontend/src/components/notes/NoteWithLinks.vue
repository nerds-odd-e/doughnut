<template>
  <NoteShell
    class="note-body"
    v-bind="{ id: note.id, updatedAt: note.noteContent?.updatedAt, language }"
  >
    <NoteFrameOfLinks v-bind="{ links: note.links }">
      <div role="title">
        <h2 class="note-title" style="display: inline-block;" @click="onTitleClick" v-if="!isEditingTitle">{{ translatedNote.title }}</h2>
        <TextInput scopeName="note" v-model="translatedNote.title" :autofocus="true" @blur="onBlurTextField" v-if="isEditingTitle" v-on:keyup.enter="$event.target.blur()"/>
      </div>
      <span 
        role="outdated-tag" 
        class="outdated-label" 
        v-if="translatedNote.isTranslationOutdatedIDN"
        >
          Outdated translation
      </span>
      <p
        style="color: red"
        role="title-fallback"
        v-if="translatedNote.translationNoteAvailable"
      >
        No translation available
      </p>
      <NoteContent v-bind="{ note, language }"/>
    </NoteFrameOfLinks>
  </NoteShell>
</template>

<script>
import TextInput from "../form/TextInput.vue";
import NoteFrameOfLinks from "../links/NoteFrameOfLinks.vue";
import NoteShell from "./NoteShell.vue";
import NoteContent from "./NoteContent.vue";
import { TranslatedNoteWrapper } from "../../models/languages";
import { storedApiUpdateNote } from "../../storedApi";

export default {
  name: "NoteWithLinks",
  props: {
    note: Object,
    language: String,
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
      formErrors: {},
    };
  },
  computed: {
    translatedNote(){
      return new TranslatedNoteWrapper(this.note, this.language);
    },
  },
  methods: {
    onTitleClick() {
      this.isEditingTitle = true;
    },
    onBlurTextField() {
      this.isEditingTitle = false;
      this.loading = true
      storedApiUpdateNote(this.$store, this.note.id, this.note.noteContent)
      .then((res) => {
        this.$emit("done");
      })
      .catch((res) => (this.formErrors = res))
      .finally(() => this.loading = false)
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
