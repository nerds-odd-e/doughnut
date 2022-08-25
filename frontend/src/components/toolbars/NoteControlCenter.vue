<template>
  <ToolbarFrame>
    <div class="btn-group btn-group-sm">
      <NoteUndoButton
        :history-state="historyState"
        @note-realm-updated="$emit('noteRealmUpdated', $event)"
      />
    </div>
  </ToolbarFrame>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import ToolbarFrame from "./ToolbarFrame.vue";
import NoteUndoButton from "./NoteUndoButton.vue";
import { HistoryState } from "../../store/history";

export default defineComponent({
  props: {
    historyState: { type: Object as PropType<HistoryState>, required: true },
  },
  emits: ["noteDeleted", "noteRealmUpdated", "newNoteAdded"],
  components: { ToolbarFrame, NoteUndoButton },
});
</script>
