<template>
  <Breadcrumb
    v-bind="{
      owns: true,
      ancestors: noteViewedByUser.ancestors,
      notebook: noteViewedByUser.notebook,
    }"
  />
  <Card :note="noteViewedByUser.noteItself">
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
  <div :id="`note-collapse-${noteViewedByUser.noteItself.id}`" class="collapse">
    <NoteShow
      v-bind="noteViewedByUser.noteItself"/>
  </div>
</template>

<script>
import Breadcrumb from "../notes/Breadcrumb.vue";
import NoteShow from "../notes/NoteShow.vue";
import Card from "../notes/Card.vue";

export default {
  name: "LinkNoteShow",
  props: { noteViewedByUser: Object },
  components: { Breadcrumb, NoteShow, Card },
};
</script>
