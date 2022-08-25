<template>
  <div class="container">
    <NoteCardsView
      v-bind="{ noteId, updatedNoteRealm, expandChildren: true }"
    />
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import NoteCardsView from "../components/notes/views/NoteCardsView.vue";

export default defineComponent({
  props: {
    noteId: { type: Number, required: true },
    updatedNoteRealm: { type: Object },
  },
  components: { NoteCardsView },
});
</script>
