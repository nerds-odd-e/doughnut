<template>
  <NoteViewedByUserWithoutChildren
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
import NoteViewedByUserWithoutChildren from "./NoteViewedByUserWithoutChildren.vue";
import NoteOwnerViewCards from "./NoteOwnerViewCards.vue";

export default {
  name: "NoteViewedByUser",
  props: {
    id: Number,
    breadcrumb: Object,
  },
  components: {
    NoteViewedByUserWithoutChildren,
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
