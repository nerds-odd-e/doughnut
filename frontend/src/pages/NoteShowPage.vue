<template>
  <div class="container">
    <NoteCardsView
      v-bind="{
        noteId,
        expandChildren: true,
        storageAccessor,
      }"
      :key="noteId"
    />
    <AddCommentView
      v-bind="{
        noteId,
        storageAccessor,
      }"
    />
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import AddCommentView from "../components/notes/views/NoteCommentsView.vue";
import NoteCardsView from "../components/notes/views/NoteCardsView.vue";
import { StorageAccessor } from "../store/createNoteStorage";

export default defineComponent({
  props: {
    noteId: { type: Number, required: true },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  components: { NoteCardsView, AddCommentView },
});
</script>
