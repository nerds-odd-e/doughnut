<template>
  <h3 v-if="note">
    Link <strong>{{ note.topic }}</strong> to
  </h3>
  <h3 v-else>Searching</h3>
  <SearchNote
    v-if="!targetNote"
    v-bind="{ noteId: note?.id }"
    @selected="targetNote = $event"
  />
  <LinkNoteFinalize
    v-if="targetNote && note"
    v-bind="{ targetNote, note, storageAccessor }"
    @success="popup.done($event)"
    @go-back="targetNote = undefined"
  />
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import LinkNoteFinalize from "./LinkNoteFinalize.vue";
import SearchNote from "../search/SearchNote.vue";
import { StorageAccessor } from "../../store/createNoteStorage";
import asPopup from "../commons/Popups/asPopup";

export default defineComponent({
  setup() {
    return asPopup();
  },
  props: {
    note: Object as PropType<Generated.Note>,
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: { LinkNoteFinalize, SearchNote },
  data() {
    return {
      targetNote: undefined,
    } as {
      targetNote: Generated.Note | undefined;
    };
  },
});
</script>
