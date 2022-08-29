<template>
  <div class="container">
    <NoteCardsView
      v-bind="{
        noteId,
        updatedNoteRealm,
        updatedAt,
        expandChildren: true,
        storageAccessor,
      }"
    />
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteCardsView from "../components/notes/views/NoteCardsView.vue";
import { StorageAccessor } from "../store/history";

export default defineComponent({
  props: {
    noteId: { type: Number, required: true },
    updatedNoteRealm: { type: Object as PropType<Generated.NoteRealm> },
    updatedAt: { type: Date },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: { NoteCardsView },
});
</script>
