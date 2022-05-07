<template>
  <NoteRealmAsync
    v-bind="{
      noteId,
      viewType,
      expandChildren: true,
    }"
    :key="noteId"
  />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import NoteRealmAsync from "../components/notes/NoteRealmAsync.vue";

export default defineComponent({
  props: { rawNoteId: String, viewType: String },
  components: { NoteRealmAsync },
  computed: {
    computedViewType() {
      if (this.viewType === "") return "cards";
      return this.viewType;
    },
    noteId(): number {
      if (!this.rawNoteId) return Number.NaN;
      return Number.parseInt(this.rawNoteId);
    },
  },
});
</script>
