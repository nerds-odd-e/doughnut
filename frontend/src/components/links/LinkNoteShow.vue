<template>
  <Breadcrumb v-bind="notePosition" />
  <Card :note="noteItself">
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
  <div :id="`note-collapse-${noteItself.id}`" class="collapse">
    <NoteWithLinks
      v-bind="noteItself"/>
  </div>
</template>

<script>
import Breadcrumb from "../notes/Breadcrumb.vue";
import NoteWithLinks from "../notes/NoteWithLinks.vue";
import Card from "../notes/Card.vue";

export default {
  name: "LinkNoteShow",
  props: { noteItself: Object, notePosition: Object },
  components: { Breadcrumb, NoteWithLinks, Card },
};
</script>
