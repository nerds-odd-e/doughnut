<template>
  <div class="card">
    <slot name="cardHeader" />
    <div class="card-body">
      <h5>
        <NoteTopicWithLink v-bind="{ note }" class="card-title" />
      </h5>
      <NoteShortDetails :details="note.details" />
      <slot name="button" :note="note" />
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { Note } from "@/generated/backend";
import NoteTopicWithLink from "./NoteTopicWithLink.vue";
import NoteShortDetails from "./NoteShortDetails.vue";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Note>, required: true },
  },
  components: {
    NoteTopicWithLink,
    NoteShortDetails,
  },
});
</script>
