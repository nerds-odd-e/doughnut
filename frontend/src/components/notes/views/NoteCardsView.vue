<template>
  <div class="container" v-if="note">
    <NoteWithLinks v-bind="{ note }"/>
    <NoteStatisticsButton :noteId="noteId" />
    <NoteAddCommentButton v-if="featureToggle" :noteId="noteId" />
    <Cards v-if="expandChildren" :notes="children"/>
  </div>

</template>

<script>
import NoteWithLinks from "../NoteWithLinks.vue";
import NoteStatisticsButton from "../NoteStatisticsButton.vue";
import NoteAddCommentButton from "../NoteAddCommentButton.vue";
import Cards from "../Cards.vue";

export default {
  props: {
    noteId: [String, Number],
    expandChildren: { type: Boolean, required: true },
  },
  components: {
    NoteWithLinks,
    Cards,
    NoteStatisticsButton,
    NoteAddCommentButton
  },
  computed: {
    note() {
      return this.$store.getters.getNoteById(this.noteId);
    },
    children() {
      return this.$store.getters.getChildrenOfParentId(this.noteId);
    },
    featureToggle() { return this.$store.getters.getFeatureToggle()}
  }
};
</script>
