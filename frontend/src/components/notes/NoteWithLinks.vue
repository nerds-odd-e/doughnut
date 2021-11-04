<template>
    <NoteShell class="note-body" v-bind="{id, updatedAt: noteContent.updatedAt}">
      <NoteFrameOfLinks v-bind="{ links }">
        <h2 role="title" class="note-title">{{ title }}</h2>
        <NoteBody v-bind="{notePicture, noteContent}"/>
      </NoteFrameOfLinks>
    </NoteShell>
</template>

<script>
import NoteFrameOfLinks from "../links/NoteFrameOfLinks.vue";
import NoteShell from "./NoteShell.vue";
import NoteBody from "./NoteBody.vue";

export default {
  name: "NoteWithLinks",
  props: {
    id: [String, Number],
    noteContent: { type: Object, required: true },
    title: String,
    notePicture: String,
    links: Object,
  },
  components: {
    NoteFrameOfLinks,
    NoteShell,
    NoteBody,
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
