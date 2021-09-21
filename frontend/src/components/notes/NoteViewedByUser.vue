<template>
  <NoteViewedByUserWithoutChildren
    v-bind="{ note, links, ancestors, notebook, owns }"
    @updated="$emit('updated')"
  />
  <NoteOwnerViewCards
    :owns="owns"
    :notes="children"
    @updated="$emit('updated')"
  />

  <router-link
    :to="{ name: 'noteOverview', params: { noteId: note.id } }"
    v-if="!!note.id"
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
    note: Object,
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
};
</script>
