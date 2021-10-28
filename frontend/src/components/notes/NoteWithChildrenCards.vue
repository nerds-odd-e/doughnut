<template>
  <NoteWithControls
    v-bind="{
      id,
      note,
      notePosition }"
  />
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
import NoteWithControls from "./NoteWithControls.vue";
import NoteOwnerViewCards from "./NoteOwnerViewCards.vue";

export default {
  name: "NoteViewedByUser",
  props: {
    id: [String, Number],
    notePosition: Object,
  },
  components: {
    NoteWithControls,
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
