<template>
  <LoadingPage v-bind="{ loading, contentExists: !!true }">
    <NoteSphereComponent
      v-bind="{
        noteId,
        viewType,
        expandChildren: true,
        noteComponent: viewTypeObj.noteComponent}"
      :key="noteId"
      />

  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import LoadingPage from "./commons/LoadingPage.vue";
import NoteSphereComponent from '../components/notes/views/NoteSphereComponent.vue';
import { ViewType, viewType } from "../models/viewTypes";
import useStoredLoadingApi from "../managedApi/useStoredLoadingApi";

export default defineComponent({
  setup() {
    return useStoredLoadingApi({initalLoading: true});
  },
  name: "NoteShowPage",
  props: { rawNoteId: String, viewType: String },
  components: { LoadingPage, NoteSphereComponent },
  computed: {
    noteId(): number {
      if(!this.rawNoteId) return Number.NaN;
      return Number.parseInt(this.rawNoteId)
    },
    viewTypeObj() : ViewType {
      return viewType(this.viewType)
    }
  },
  methods: {
    fetchData() {
      const storedApiCall = this.viewTypeObj.fetchAll ?
                              this.storedApi.getNoteWithDescendents :
                              this.storedApi.getNoteAndItsChildren

      storedApiCall(this.noteId)
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
});

</script>