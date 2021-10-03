<template>
  <NoteWithControls
    v-bind="{
      id,
      note,
      breadcrumb }"
  />
  <NoteOwnerViewCards
    :owns="breadcrumb.owns"
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
</template>

<script>
import NoteWithControls from "./NoteWithControls.vue";
import NoteOwnerViewCards from "./NoteOwnerViewCards.vue";

export default {
  name: "NoteViewedByUser",
  props: {
    id: Number,
    breadcrumb: Object,
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
