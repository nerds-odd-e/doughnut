<template>
  <div class="card">
    <slot name="cardHeader" />
    <div class="card-body">
      <h5 class="card-title">
        <NoteTitleWithLink :note="note" class="card-title" />
      </h5>
      <NoteShortDescription :description="note.textContent.description" />
      <slot name="button" :note="note" />
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteTitleWithLink from "./NoteTitleWithLink.vue";
import NoteShortDescription from "./NoteShortDescription.vue";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Generated.Note>, required: true },
  },
  components: {
    NoteTitleWithLink,
    NoteShortDescription,
  },
});
</script>
