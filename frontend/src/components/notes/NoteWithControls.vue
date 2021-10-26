<template>
  <Breadcrumb v-bind="notePosition" />
  <NoteWithLinks v-bind="note"/>
</template>

<script>
import NoteWithLinks from "./NoteWithLinks.vue";
import Breadcrumb from "./Breadcrumb.vue";

export default {
  name: "NoteWithControls",
  props: {
    id: [String, Number],
    note: Object,
    notePosition: Object,
  },
  components: {
    NoteWithLinks,
    Breadcrumb,
  },
};
</script>
