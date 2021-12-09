<template>
  <NoteShell
    class="note-body"
    v-bind="{ id: note.id, updatedAt: note.noteContent?.updatedAt, language }"
  >
    <NoteFrameOfLinks v-bind="{ links: note.links }">
      <h2 role="title" class="note-title" @click="onTitleClick" v-if="!isEditingTitle">{{ translatedNote.title }}</h2>
      <TextInput scopeName="note" v-model="translatedNote.title" :autofocus="true" @blur="onBlurTextField" v-if="isEditingTitle"/>
      <p
        style="color: red"
        role="title-fallback"
        v-if="translatedNote.translationNoteAvailable"
      >
        No translation available
      </p>
      <NoteContent v-bind="{ note, language, isInPlaceEditEnabled }"/>
    </NoteFrameOfLinks>
  </NoteShell>
</template>

<script>
import TextInput from "../form/TextInput.vue";
import NoteFrameOfLinks from "../links/NoteFrameOfLinks.vue";
import NoteShell from "./NoteShell.vue";
import NoteContent from "./NoteContent.vue";
import { TranslatedNoteWrapper } from "../../models/languages";

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
      this.note.title = input.target.value;
      this.isEditingTitle = false;
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
</style>
