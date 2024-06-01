<template>
  <div class="review-point-abbr">
    <label><strong>Review Point: </strong></label>

    <NoteTopicComponent
      v-if="note.noteTopic.targetNoteTopic"
      :note-topic="note.noteTopic.parentNoteTopic!"
    />
    <NoteTopicComponent :note-topic="note.noteTopic" />
  </div>
</template>

<script setup lang="ts">
import { Note } from "@/generated/backend";
import { PropType } from "vue";
import NoteTopicComponent from "../notes/core/NoteTopicComponent.vue";

defineProps({
  note: {
    type: Object as PropType<Note>,
    required: true,
  },
});
</script>

<style lang="sass" scoped>
.review-point-abbr
  border: 1px solid #ccc
  width: 100%
  border-radius: 5px
  padding: 2px
</style>
