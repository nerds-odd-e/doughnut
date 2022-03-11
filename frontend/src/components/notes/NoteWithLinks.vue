<template>
  <NoteShell
    v-if="note"
    class="note-body"
    v-bind="{ id: note.id, updatedAt: note.textContent?.updatedAt }"
  >
    <NoteFrameOfLinks v-bind="{ links: note.links }">
      <NoteContent v-bind="{ note }" />
    </NoteFrameOfLinks>
  </NoteShell>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import EditableText from "../form/EditableText.vue";
import NoteFrameOfLinks from "../links/NoteFrameOfLinks.vue";
import NoteShell from "./NoteShell.vue";
import NoteContent from "./NoteContent.vue";
import useStoredLoadingApi from '../../managedApi/useStoredLoadingApi';

export default defineComponent({
  setup() {
    return useStoredLoadingApi();
  },
  name: "NoteWithLinks",
  props: {
    noteId: { type: Number, required: true },
  },
  components: {
    NoteFrameOfLinks,
    NoteShell,
    NoteContent,
    EditableText,
  },
  computed: {
    note() {
      return this.piniaStore.getNoteById(this.noteId);
    },
  }
});
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
