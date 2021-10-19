<template>
  <Breadcrumb v-bind="notePosition" />
  <div class="note-with-controls">
    <nav class="nav d-flex flex-row-reverse p-0">
      <NoteButtons
        v-if="notePosition.owns"
        :note="note"
        :addSibling="true"
      />
      <BazaarNoteButtons v-else :note="note" :notebook="notePosition.notebook" />
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
    id: String,
    note: Object,
    notePosition: Object,
  },
  components: {
    NoteWithLinks,
    BazaarNoteButtons,
    NoteButtons,
    Breadcrumb,
  },
};
</script>
