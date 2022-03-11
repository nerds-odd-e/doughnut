<template>
  <Breadcrumb v-bind="notePosition" />
  <Card :note="note">
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

<script>
import Breadcrumb from "../notes/Breadcrumb.vue";
import NoteWithLinks from "../notes/NoteWithLinks.vue";
import Card from "../notes/Card.vue";

export default {
  name: "LinkNoteShow",
  props: { note: Object, notePosition: Object },
  components: { Breadcrumb, NoteWithLinks, Card },
};
</script>
