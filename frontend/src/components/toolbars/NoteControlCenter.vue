<template>
  <ToolbarFrame>
    <div class="btn-group btn-group-sm">
      <NoteUndoButton @note-realm-updated="$emit('noteRealmUpdated', $event)" />
    </div>
  </ToolbarFrame>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import ToolbarFrame from "./ToolbarFrame.vue";
import NoteUndoButton from "./NoteUndoButton.vue";

export default defineComponent({
  emits: ["noteDeleted", "noteRealmUpdated", "newNoteAdded"],
  components: { ToolbarFrame, NoteUndoButton },
});
</script>
