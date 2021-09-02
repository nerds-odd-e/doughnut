<template>
  <NoteViewedByUserWithoutChildren
    v-bind="{ note, links, ancestors, notebook, owns }"
    @updated="$emit('updated')"
  />
  <p
    data-testid="overview-note-title"
    class="overview-note-title"
    v-for="child in children"
    :key="child.noteId"
  >
    {{ child.title }}
  </p>
</template>

<script>
import NoteViewedByUserWithoutChildren from "./NoteViewedByUserWithoutChildren.vue";

export default {
  name: "NoteOverview",
  props: {
    note: Object,
    links: Object,
    children: Array,
    ancestors: Array,
    notebook: Object,
    owns: { type: Boolean, required: true },
  },
  emits: ["updated"],
  components: { NoteViewedByUserWithoutChildren },
};
</script>
