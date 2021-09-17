<template>
  <NoteOwnerBreadcrumb
    v-if="owns"
    :ancestors="ancestors"
    :notebook="notebook"
  />
  <NoteBazaarBreadcrumb v-else :ancestors="ancestors">
  </NoteBazaarBreadcrumb>
</template>

<script>
import NoteBazaarBreadcrumb from "../bazaar/NoteBazaarBreadcrumb.vue";
import NoteOwnerBreadcrumb from "./NoteOwnerBreadcrumb.vue";

export default {
  name: "Breadcrumb",
  props: {
    ancestors: Array,
    notebook: Object,
    noteTitle: String,
    owns: { type: Boolean, required: true },
  },
  components: {
    NoteBazaarBreadcrumb,
    NoteOwnerBreadcrumb,
  },
};
</script>
