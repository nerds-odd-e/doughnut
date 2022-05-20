<template>
  <BreadcrumbAsync v-bind="{ noteId: note.id }" />
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
import BreadcrumbAsync from "../toolbars/BreadcrumbAsync.vue";
import Card from "../notes/Card.vue";

export default defineComponent({
  name: "LinkNoteShow",
  props: {
    note: { type: Object as PropType<Generated.NoteRealm>, required: true },
    notePosition: Object as PropType<Generated.NotePositionViewedByUser>,
  },
  components: { BreadcrumbAsync, Card },
});
</script>
