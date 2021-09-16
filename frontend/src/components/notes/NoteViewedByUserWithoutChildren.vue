<template>
  <NoteOwnerBreadcrumb
    v-if="owns"
    :ancestors="ancestors"
    :notebook="notebook"
  />
  <NoteBazaarBreadcrumb v-else :ancestors="ancestors">
    <li class="breadcrumb-item">{{ note.title }}</li>
  </NoteBazaarBreadcrumb>
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
      :level="1"
      :staticInfo="$staticInfo"
      @updated="$emit('updated')"
    />
  </div>
</template>

<script>
import NoteShow from "./NoteShow.vue";
import NoteBazaarBreadcrumb from "../bazaar/NoteBazaarBreadcrumb.vue";
import BazaarNoteButtons from "../bazaar/BazaarNoteButtons.vue";
import NoteButtons from "./NoteButtons.vue";
import NoteOwnerBreadcrumb from "./NoteOwnerBreadcrumb.vue";

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
    NoteBazaarBreadcrumb,
    BazaarNoteButtons,
    NoteButtons,
    NoteOwnerBreadcrumb,
  },
};
</script>
