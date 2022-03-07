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
import useStoredLoadingApi from "../../../managedApi/useStoredLoadingApi";

export default ({
  setup() {
    return useStoredLoadingApi();
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
      return this.piniaStore.getNoteById(this.noteId);
    },
    children() {
      return this.piniaStore.getChildrenOfParentId(this.noteId);
    },
  }
});
</script>
