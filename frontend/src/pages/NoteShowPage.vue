<template>
  <LoadingPage v-bind="{ loading, contentExists: !!notePosition }">
    <NotePageFrame
      v-bind="{
        noteId,
        notePosition,
        viewType,
        expandChildren: true,
        noteComponent: viewTypeObj.noteComponent}"/>

    <NoteStatisticsButton :noteId="noteId" />
  </LoadingPage>
</template>

<script>
import LoadingPage from "./commons/LoadingPage.vue";
import NotePageFrame from '../components/notes/views/NotePageFrame.vue';
import NoteStatisticsButton from "../components/notes/NoteStatisticsButton.vue";
import { storedApiGetNoteAndItsChildren, storedApiGetNoteWithDescendents } from "../storedApi";
import { viewType } from "../models/viewTypes";

export default {
  name: "NoteShowPage",
  props: { noteId: [String, Number], viewType: String },
  data() {
    return {
      notePosition: null,
      loading: true,
    };
  },
  components: { LoadingPage, NotePageFrame, NoteStatisticsButton },
  computed: {
    viewTypeObj() {
      return viewType(this.viewType)
    }
  },
  methods: {
    fetchData() {
      this.loading = true
      const storedApiCall = this.viewTypeObj.fetchAll ?
                              storedApiGetNoteWithDescendents :
                              storedApiGetNoteAndItsChildren

      storedApiCall(this.$store, this.noteId)
      .then((res) => {
        this.notePosition = res.notePosition;
      }).finally(() => this.loading = false);
    },
  },
  watch: {
    noteId() {
      this.fetchData();
    },
    viewType() {
      this.fetchData();
    },
  },
  mounted() {
    this.fetchData();
  },
};
</script>