<template>
    <NoteShell class="note-body" v-bind="{id: note.id, updatedAt: note.noteContent?.updatedAt}">
      <NoteFrameOfLinks v-bind="{ links: note.links }">
        <h2 role="title" class="note-title">{{ translatedTitle }}</h2>
        <p role="title-fallback" v-if="!note.noteContent.titleIDN">No translation available</p>
        <NoteContent v-bind="{note}"/>
      </NoteFrameOfLinks>
    </NoteShell>
</template>

<script>
import NoteFrameOfLinks from "../links/NoteFrameOfLinks.vue";
import NoteShell from "./NoteShell.vue";
import NoteContent from "./NoteContent.vue";
import Languages from "../../constants/lang";

export default {
  name: "NoteWithLinks",
  props: {
    note: Object,
  },
  components: {
    NoteFrameOfLinks,
    NoteShell,
    NoteContent,
  },
  computed: {
    translatedTitle(){
      if (!this.note.noteContent)
        return this.note.title;
      
      return this.note.language === Languages.ID && this.note.noteContent.titleIDN ? this.note.noteContent.titleIDN : this.note.noteContent.title;
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
