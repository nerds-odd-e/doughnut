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
import storedApi from  "../managedApi/storedApi";
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
    fetchData() {
      this.loading = true;
      const storedApiCall = this.viewTypeObj.fetchAll ?
                              storedApi(this.$store).getNoteWithDescendents :
                              storedApi(this.$store).getNoteAndItsChildren

      storedApiCall(this.noteId)
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
      this.updateStoreViewType();
      this.fetchData();
    },
  },
  mounted() {
    this.updateStoreViewType();
    this.fetchData();
  },
};

</script>