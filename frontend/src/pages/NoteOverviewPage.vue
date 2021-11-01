<template>
  <LoadingPage v-bind="{ loading, contentExists: !!notePosition, inContainer: true }">
    <NotePageFrame
      v-bind="{
        noteId,
        notePosition,
        deleteRedirect: false,
        noteRouteName: 'noteOverview',
        noteComponent: 'NoteOverview'}"/>
  </LoadingPage>
</template>

<script>
import LoadingPage from "./commons/LoadingPage.vue";
import NotePageFrame from '../components/notes/NotePageFrame.vue';
import { storedApiGetNoteWithDescendents } from "../storedApi";

export default {
  name: "NoteOverviewPage",
  props: { noteId: String },
  data() {
    return {
      notePosition: null,
      loading: true,
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