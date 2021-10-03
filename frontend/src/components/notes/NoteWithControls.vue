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
    <NoteWithLinks v-bind="note"/>
  </div>
</template>

<script>
import NoteWithLinks from "./NoteWithLinks.vue";
import Breadcrumb from "./Breadcrumb.vue";
import BazaarNoteButtons from "../bazaar/BazaarNoteButtons.vue";
import NoteButtons from "./NoteButtons.vue";

export default {
  name: "NoteWithControls",
  props: {
    id: Number,
    note: Object,
    breadcrumb: Object,
  },
  components: {
    NoteWithLinks,
    BazaarNoteButtons,
    NoteButtons,
    Breadcrumb,
  },
};
</script>
