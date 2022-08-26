<template>
  <div class="container">
    <NoteCardsView
      v-bind="{ noteId, updatedNoteRealm, updatedAt, expandChildren: true }"
    />
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteCardsView from "../components/notes/views/NoteCardsView.vue";

export default defineComponent({
  props: {
    noteId: { type: Number, required: true },
    updatedNoteRealm: { type: Object as PropType<Generated.NoteRealm> },
    updatedAt: { type: Date },
  },
  components: { NoteCardsView },
});
</script>
