<template>
  <LoadingPage v-bind="{ loading, contentExists: !!notePosition }">
    <NoteSphereComponent
      v-bind="{
        noteId,
        notePosition,
        expandChildren: true,
        noteComponent: viewTypeObj.noteComponent}"
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
  data() {
    return {
      notePosition: null,
    } as {
      notePosition: Generated.NotePositionViewedByUser | null
    };
  },
  components: { LoadingPage, NoteSphereComponent },
  computed: {
    noteId() {
      if(!this.rawNoteId) return Number.NaN;
      return Number.parseInt(this.rawNoteId)
    },
    viewTypeObj() : ViewType {
      return viewType(this.viewType)
    }
  },
  methods: {
    updateStoreViewType() {
      if(!this.viewType) return;
      this.piniaStore.setViewType(this.viewType);
    },
    fetchData() {
      const storedApiCall = this.viewTypeObj.fetchAll ?
                              this.storedApi.getNoteWithDescendents :
                              this.storedApi.getNoteAndItsChildren

      storedApiCall(this.noteId)
      .then((res) => {
        this.notePosition = res.notePosition;
      })
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
});

</script>