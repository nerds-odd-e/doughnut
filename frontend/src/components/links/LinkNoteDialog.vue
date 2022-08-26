<template>
  <h3 v-if="note">
    Link <strong>{{ note.title }}</strong> to
  </h3>
  <h3 v-else>Searching</h3>
  <SearchNote
    v-if="!targetNote"
    v-bind="{ noteId: note?.id }"
    @selected="targetNote = $event"
  />
  <LinkNoteFinalize
    v-if="targetNote && note"
    v-bind="{ targetNote, note, historyWriter }"
    @success="$emit('done', $event)"
    @go-back="targetNote = undefined"
  />
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import LinkNoteFinalize from "./LinkNoteFinalize.vue";
import SearchNote from "../search/SearchNote.vue";
import { HistoryWriter } from "../../store/history";

export default defineComponent({
  props: {
    note: Object as PropType<Generated.Note>,
    historyWriter: {
      type: Function as PropType<HistoryWriter>,
      required: true,
    },
  },
  components: { LinkNoteFinalize, SearchNote },
  emits: ["done"],
  data() {
    return {
      targetNote: undefined,
    } as {
      targetNote: Generated.Note | undefined;
    };
  },
});
</script>
