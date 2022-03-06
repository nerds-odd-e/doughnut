<template>
  <div class="container" v-if="note">
    <NoteWithLinks v-bind="{ note }"/>
    <NoteStatisticsButton :noteId="noteId" />
    <Cards v-if="expandChildren" :notes="children"/>
  </div>

</template>

<script>
import NoteWithLinks from "../NoteWithLinks.vue";
import NoteStatisticsButton from "../NoteStatisticsButton.vue";
import Cards from "../Cards.vue";
import { useStore } from "@/store";

export default {
  setup() {
    const store = useStore()
    return { store }
  },
  props: {
    noteId: [String, Number],
    expandChildren: { type: Boolean, required: true },
  },
  components: {
    NoteWithLinks,
    Cards,
    NoteStatisticsButton,
  },
  computed: {
    note() {
      return this.store.getNoteById(this.noteId);
    },
    children() {
      return this.store.getChildrenOfParentId(this.noteId);
    },
  }
};
</script>
