<template>
  <div class="container" v-if="note">
    <NoteWithLinks v-bind="{ note, language }" @on-editing="onEditing"/>
    <NoteStatisticsButton :noteId="noteId" />
    <Cards v-if="expandChildren" :notes="children"/>
  </div>

</template>

<script>
import NoteWithLinks from "../NoteWithLinks.vue";
import NoteStatisticsButton from "../NoteStatisticsButton.vue";
import Cards from "../Cards.vue";

export default {
  props: {
    noteId: [String, Number],
    expandChildren: { type: Boolean, required: true },
    language: String,
  },
  components: {
    NoteWithLinks,
    Cards,
    NoteStatisticsButton,
  },
  computed: {
    note() {
      return this.$store.getters.getNoteById(this.noteId);
    },
    children() {
      return this.$store.getters.getChildrenOfParentId(this.noteId);
    },
  },
  methods:{
    onEditing(value){
      this.$emit("on-editing", value);
    }
  }
};
</script>
