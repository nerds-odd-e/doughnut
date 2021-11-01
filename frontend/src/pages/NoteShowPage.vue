<template>
  <LoadingPage v-bind="{ loading, contentExists: !!notePosition, inContainer: true }">
    <NotePageFrame
      v-bind="{
        noteId,
        notePosition,
        deleteRedirect: true,
        noteRouteName: 'noteShow',
        noteComponent: 'NoteWithChildrenCards'}"/>
    <NoteStatisticsButton :noteId="noteId" />
  </LoadingPage>
</template>

<script>
import LoadingPage from "./commons/LoadingPage.vue";
import NotePageFrame from '../components/notes/NotePageFrame.vue';
import NoteStatisticsButton from "../components/notes/NoteStatisticsButton.vue";
import { storedApiGetNoteAndItsChildren } from "../storedApi";

export default {
  name: "NoteShowPage",
  props: { noteId: [String, Number] },
  data() {
    return {
      notePosition: null,
      loading: true,
    };
  },
  components: { LoadingPage, NotePageFrame, NoteStatisticsButton },
  methods: {
    fetchData() {
      this.loading = true
      storedApiGetNoteAndItsChildren(this.$store, this.noteId)
      .then((res) => {
        this.notePosition = res.notePosition;
      }).finally(() => this.loading = false);
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