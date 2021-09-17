<template>
  <Breadcrumb v-bind="{owns, ancestors, notebook, noteTitle: note.title}"/>
  <div class="note-with-controls">
    <nav class="nav d-flex flex-row-reverse p-0">
      <NoteButtons
        v-if="owns"
        :note="note"
        @updated="$emit('updated')"
        :addSibling="true"
      />
      <BazaarNoteButtons v-else :note="note" :notebook="notebook" />
    </nav>
    <NoteShow
      :note="note"
      :links="links"
      :recentlyUpdated="recentlyUpdated"
      @updated="$emit('updated')"
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
    note: Object,
    links: Object,
    ancestors: Array,
    notebook: Object,
    recentlyUpdated: Boolean,
    owns: { type: Boolean, required: true },
  },
  emits: ["updated"],
  components: {
    NoteShow,
    BazaarNoteButtons,
    NoteButtons,
    Breadcrumb,
  },
};
</script>
