<template>
  <NoteRealmAsync
    v-bind="{
      noteId,
      viewType: sanitizedViewType,
      expandChildren: true,
    }"
    :key="noteId"
  />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { sanitizeViewTypeName } from "../models/viewTypes";
import NoteRealmAsync from "../components/notes/NoteRealmAsync.vue";

export default defineComponent({
  props: { rawNoteId: String, viewType: String },
  components: { NoteRealmAsync },
  computed: {
    sanitizedViewType() {
      return sanitizeViewTypeName(this.viewType);
    },
    noteId(): number {
      if (!this.rawNoteId) return Number.NaN;
      return Number.parseInt(this.rawNoteId);
    },
  },
});
</script>
