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
    v-else
    v-bind="{ targetNote, note }"
    @success="$emit('done', $event)"
    @goBack="targetNote = undefined"
  />
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import LinkNoteFinalize from "./LinkNoteFinalize.vue";
import SearchNote from "../search/SearchNote.vue";

export default defineComponent({
  props: { note: Object as PropType<Generated.Note> },
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
