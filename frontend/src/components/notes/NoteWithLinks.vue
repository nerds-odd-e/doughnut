<template>
  <NoteShell
    class="note-body"
    v-bind="{ id: note.id, updatedAt: note.noteContent?.updatedAt, language }"
  >
    <NoteFrameOfLinks v-bind="{ links: note.links }">
      <div @click="inPlaceButton" v-if="!isEditingTitle">
        <h2 role="title" class="note-title">{{ translatedNote.title }}</h2>
      </div>
      <div @click="inPlaceButton" v-if="isEditingTitle">
        <TextInput scopeName="note" v-model="translatedNote.title" :autofocus="true"/>
      </div>
      <p
        style="color: red"
        role="title-fallback"
        v-if="translatedNote.translationNoteAvailable"
      >
        No translation available
      </p>
      <NoteContent v-bind="{ note, language, isEditingTitle}" />
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
      return new TranslatedNoteWrapper(this.note, this.language, this.isEditingTitle);
    },
  },
  methods: {
    inPlaceButton() {
      if (this.isEditingTitle) {
        this.isEditingTitle = false;
      } else{
        this.isEditingTitle = true;
      }
    },
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
