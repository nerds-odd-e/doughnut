<template>
  <div class="review-point-abbr">
    <label><strong>Review Point: </strong></label>

    <NoteTopic
      v-if="note.noteTopic.targetNoteTopic"
      :note-topic="note.noteTopic.parentNoteTopic!"
    />
    <NoteTopic :note-topic="note.noteTopic" />
  </div>
</template>

<script setup lang="ts">
import { Note } from "@/generated/backend";
import { PropType } from "vue";
import NoteTopic from "../notes/core/NoteTopic.vue";

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
