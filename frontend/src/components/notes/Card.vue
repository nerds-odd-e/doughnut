<template>
  <router-link
    :to="{ name: 'noteShow', params: { noteId: note.id } }"
    class="text-decoration-none"
  >
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
  </router-link>
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

<style scoped>
.card:hover {
  background-color: #f8f9fa !important;
}
</style>
