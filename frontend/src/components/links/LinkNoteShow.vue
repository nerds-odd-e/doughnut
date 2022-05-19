<template>
  <Breadcrumb v-bind="notePosition" />
  <Card :note="note.note">
    <template #button>
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
import { defineComponent, PropType } from "vue";
import Breadcrumb from "../toolbars/Breadcrumb.vue";
import NoteWithLinks from "../notes/NoteWithLinks.vue";
import Card from "../notes/Card.vue";

export default defineComponent({
  name: "LinkNoteShow",
  props: {
    note: { type: Object as PropType<Generated.NoteRealm>, required: true },
    notePosition: Object as PropType<Generated.NotePositionViewedByUser>,
  },
  components: { Breadcrumb, NoteWithLinks, Card },
});
</script>
