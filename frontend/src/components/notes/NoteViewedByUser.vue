<template>
  <NoteViewedByUserWithoutChildren
    v-bind="{ ...noteViewedByUser, ancestors, notebook, owns }"
    @updated="$emit('updated')"
  />
  <NoteOwnerViewCards
    :owns="owns"
    :notes="children"
    @updated="$emit('updated')"
  />

  <router-link
    :to="{ name: 'noteOverview', params: { noteId: noteViewedByUser.id } }"
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
    noteId: Number,
    links: Object,
    children: Array,
    ancestors: Array,
    notebook: Object,
    owns: { type: Boolean, required: true },
  },
  emits: ["updated"],
  components: {
    NoteViewedByUserWithoutChildren,
    NoteOwnerViewCards,
  },
  computed: {
    noteViewedByUser() {
      return this.$store.getters.getNoteById(this.noteId);
    },
  },
};
</script>
