<template>
  <div class="review-point-abbr">
    <label><strong>Review Point: </strong></label>

    <span v-if="thing.linkType">
      <span>
        {{ sourceNoteTitle }}
      </span>
      <span class="badge mr-1"> {{ thing.linkType }}</span>
      <span>
        {{ targetNoteTitle }}
      </span>
    </span>
    <span v-else>
      {{ noteTitle }}
    </span>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";

export default defineComponent({
  props: {
    thing: {
      type: Object as PropType<Generated.Thing>,
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
    sourceNoteTitle() {
      return this.thing.sourceNote?.topic;
    },
    targetNoteTitle() {
      return this.thing.targetNote?.topic;
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
