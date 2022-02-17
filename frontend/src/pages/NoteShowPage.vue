<template>
  <LoadingPage v-bind="{ loading, contentExists: !!notePosition }">
    <NotePageFrame
      v-bind="{
        noteId,
        notePosition,
        expandChildren: true,
        noteComponent: viewTypeObj.noteComponent}"
      />

  </LoadingPage>
</template>

<script>
import LoadingPage from "./commons/LoadingPage.vue";
import NotePageFrame from '../components/notes/views/NotePageFrame.vue';
import { storedApi } from "../storedApi";
import { viewType } from "../models/viewTypes";

export default {
  name: "NoteShowPage",
  props: { noteId: [String, Number], viewType: String },
  data() {
    return {
      notePosition: null,
      loading: true,
      polling: null
    };
  },
  components: { LoadingPage, NotePageFrame },
  computed: {
    viewTypeObj() {
      return viewType(this.viewType)
    }
  },
  methods: {
    updateStoreViewType() {
      this.$store.commit('viewType', this.viewType);
    },
    fetchData(loading, fetchAll) {
      this.loading = loading ?? true;
      const storedApiCall = fetchAll ?
                              storedApi(this.$store).getNoteWithDescendents :
                              storedApi(this.$store).getNoteAndItsChildren

      storedApiCall(this.noteId)
      .then((res) => {
        this.notePosition = res.notePosition;
      }).finally(() => this.loading = false);
    },
    pollData(fetchAll) {
        this.fetchData(false,fetchAll);
    },
  },
  watch: {
    noteId() {
      this.fetchData(this.loading,this.viewTypeObj.fetchAll);
    },
    viewType() {
      this.updateStoreViewType();
      this.fetchData(this.loading,this.viewTypeObj.fetchAll);
    },
  },
  mounted() {
    this.updateStoreViewType();
    this.pollData(this.viewTypeObj.fetchAll);
  },
};

</script>