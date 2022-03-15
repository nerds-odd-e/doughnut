<template>
  <Breadcrumb v-bind="notePosition" :noteId="note.id" />
  <Card :note="note.note">
    <template #button="{ note }">
      <button
        class="source_btn btn btn-sm btn-secondary"
        data-bs-toggle="collapse"
        :data-bs-target="`#note-collapse-${note.id}`"
        aria-expanded="false"
      >
        Toggle Details
      </button>
    </template>
  </Card>
</template>

<script lang="ts">
import Breadcrumb from "../notes/Breadcrumb.vue";
import NoteWithLinks from "../notes/NoteWithLinks.vue";
import Card from "../notes/Card.vue";
import { defineComponent, PropType } from "vue";

export default defineComponent({
  name: "LinkNoteShow",
  props: {
    note: { type: Object as PropType<Generated.NoteSphere>, required: true },
    notePosition: Object as PropType<Generated.NotePositionViewedByUser>
  },
  components: { Breadcrumb, NoteWithLinks, Card },
});
</script>
