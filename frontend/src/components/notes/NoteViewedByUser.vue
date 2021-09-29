<template>
  <NoteViewedByUserWithoutChildren
    v-bind="{ ...noteViewedByUser, breadcrumb }"
  />
  <NoteOwnerViewCards
    :owns="breadcrumb.owns"
    :notes="children"
  />

  <router-link
    :to="{ name: 'noteOverview', params: { noteId: id } }"
    v-if="!!noteViewedByUser && !!noteViewedByUser.id"
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
    noteViewedByUser() {
      return this.$store.getters.getNoteById(this.id);
    },
    children() {
      return this.$store.getters.getChildrenOfParentId(this.id);
    },
  },
};
</script>
