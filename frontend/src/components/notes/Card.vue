<template>
  <div class="card">
    <slot name="cardHeader" />
    <div class="card-body">
      <h5 class="card-title">
        <NoteTopicWithLink v-bind="{ note, parentNote }" class="card-title" />
      </h5>
      <NoteShortDetails :details="note.details" />
      <slot name="button" :note="note" />
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteTopicWithLink from "./NoteTopicWithLink.vue";
import NoteShortDetails from "./NoteShortDetails.vue";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Generated.Note>, required: true },
    parentNote: { type: Object as PropType<Generated.Note> },
  },
  components: {
    NoteTopicWithLink,
    NoteShortDetails,
  },
});
</script>
