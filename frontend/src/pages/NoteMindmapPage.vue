<template>
  <LoadingPage v-bind="{ loading, contentExists: !!notePosition }">
    <NotePageFrame
      v-bind="{
        noteId,
        notePosition,
        deleteRedirect: false,
        expendChildren: true,
        noteRouteName: 'mindmap',
        noteComponent: 'NoteMindmapWithListner'}"/>
  </LoadingPage>
</template>

<script>
import LoadingPage from "./commons/LoadingPage.vue";
import { storedApiGetNoteWithDescendents } from "../storedApi";
import NotePageFrame from '../components/notes/NotePageFrame.vue';

export default {
  props: { noteId: [String, Number] },
  data() {
    return {
      loading: false,
      notePosition: null,
      highlightNoteId: null,
    };
  },
  components: { LoadingPage, NotePageFrame },
  methods: {
    fetchData() {
      this.loading = true;
      storedApiGetNoteWithDescendents(this.$store, this.noteId)
      .then((res) => {
        this.notePosition = res.notePosition;
      })
      .finally(() => this.loading = false)
      ;
    },
  },
  watch: {
    noteId() {
      this.fetchData();
    },
  },
  mounted() {
    this.fetchData();
  },
};
</script>