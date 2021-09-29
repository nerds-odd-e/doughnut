<template>
  <Breadcrumb v-bind="breadcrumb" />
  <div class="note-with-controls">
    <nav class="nav d-flex flex-row-reverse p-0">
      <NoteButtons
        v-if="breadcrumb.owns"
        :note="note"
        :addSibling="true"
      />
      <BazaarNoteButtons v-else :note="note" :notebook="breadcrumb.notebook" />
    </nav>
    <NoteShow
      :id="id"
      :note="note"
      :links="links"
    />
  </div>
</template>

<script>
import NoteShow from "./NoteShow.vue";
import Breadcrumb from "./Breadcrumb.vue";
import BazaarNoteButtons from "../bazaar/BazaarNoteButtons.vue";
import NoteButtons from "./NoteButtons.vue";

export default {
  name: "NoteViewedByUserWithoutChildren",
  props: {
    id: Number,
    note: Object,
    links: Object,
    breadcrumb: Object,
  },
  components: {
    NoteShow,
    BazaarNoteButtons,
    NoteButtons,
    Breadcrumb,
  },
};
</script>
