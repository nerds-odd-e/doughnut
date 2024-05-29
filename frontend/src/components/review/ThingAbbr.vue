<template>
  <div class="review-point-abbr">
    <label><strong>Review Point: </strong></label>

    <span v-if="note.noteTopic.targetNoteTopic">
      <NoteTopic :note-topic="note.noteTopic.parentNoteTopic" />
      <NoteTopic :note-topic="note.noteTopic.targetNoteTopic" />
    </span>
    <span v-else>
      {{ noteTitle }}
    </span>
  </div>
</template>

<script lang="ts">
import { Note } from "@/generated/backend";
import { defineComponent, PropType } from "vue";
import NoteTopic from "../notes/core/NoteTopic.vue";

export default defineComponent({
  props: {
    note: {
      type: Object as PropType<Note>,
      required: true,
    },
  },
  computed: {
    noteTitle() {
      return this.note?.topic;
    },
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
