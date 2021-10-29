<template>
  <Breadcrumb v-bind="notePosition" />
  <NoteWithLinks v-bind="note"/>
  <NoteOwnerViewCards
    :owns="notePosition.owns"
    :notes="children"
  />

  <router-link
    :to="{ name: 'noteOverview', params: { noteId: id } }"
    v-if="!!note && !!note.id"
    role="button"
    class="btn btn-sm"
  >
    Full view mode
  </router-link>

  <router-link
    :to="{ name: 'mindmap', params: { noteId: id } }"
    v-if="!!note && !!note.id"
    role="button"
    class="btn btn-sm"
  >
    Mindmap mode
  </router-link>

</template>

<script>
import NoteOwnerViewCards from "./NoteOwnerViewCards.vue";
import NoteWithLinks from "./NoteWithLinks.vue";
import Breadcrumb from "./Breadcrumb.vue";

export default {
  name: "NoteViewedByUser",
  props: {
    id: [String, Number],
    notePosition: Object,
  },
  components: {
    Breadcrumb,
    NoteWithLinks,
    NoteOwnerViewCards,
  },
  computed: {
    note() {
      return this.$store.getters.getNoteById(this.id);
    },
    children() {
      return this.$store.getters.getChildrenOfParentId(this.id);
    },
  },
};
</script>
