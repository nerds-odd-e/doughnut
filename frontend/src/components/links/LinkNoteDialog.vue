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
    @success="$emit('done')"
    @goBack="targetNote = null"
  />
</template>

<script>
import LinkNoteFinalize from "./LinkNoteFinalize.vue";
import SearchNote from "../search/SearchNote.vue";

export default {
  name: "LinkNote",
  props: { note: Object },
  components: { LinkNoteFinalize, SearchNote },
  emits: ["done"],
  data() {
    return {
      targetNote: null,
    };
  },
};
</script>
