<template>
  <div class="review-point-abbr">
    <label><strong>Review Point: </strong></label>

    <span v-if="thing.note?.noteTopic.targetNoteTopic">
      <NoteTopic :note-topic="thing.note.noteTopic.parentNoteTopic" />
      <NoteTopic :note-topic="thing.note.noteTopic.targetNoteTopic" />
    </span>
    <span v-else>
      {{ noteTitle }}
    </span>
  </div>
</template>

<script lang="ts">
import { Thing } from "@/generated/backend";
import { defineComponent, PropType } from "vue";
import NoteTopic from "../notes/core/NoteTopic.vue";

export default defineComponent({
  props: {
    thing: {
      type: Object as PropType<Thing>,
      required: true,
    },
  },
  computed: {
    note() {
      return this.thing.note;
    },
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
