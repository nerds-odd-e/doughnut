<template>
  <NoteViewedByUserWithoutChildren
    v-bind="{ ...noteViewedByUser, ancestors, notebook, owns }"
  />
  <NoteOwnerViewCards
    :owns="owns"
    :notes="children"
  />

  <router-link
    :to="{ name: 'noteOverview', params: { noteId: this.id } }"
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
    links: Object,
    children: Array,
    ancestors: Array,
    notebook: Object,
    owns: { type: Boolean, required: true },
  },
  components: {
    NoteViewedByUserWithoutChildren,
    NoteOwnerViewCards,
  },
  computed: {
    noteViewedByUser() {
      return this.$store.getters.getNoteById(this.id);
    },
  },
};
</script>
