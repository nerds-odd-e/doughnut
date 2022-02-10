<template>
  <LoadingPage v-bind="{ loading, contentExists: !!notePosition }">
    <NotePageFrame
      v-bind="{
        noteId,
        notePosition,
        viewType,
        expandChildren: true,
        noteComponent: viewTypeObj.noteComponent}"
      @on-editing="onEditing" />

  </LoadingPage>
</template>

<script>
import LoadingPage from "./commons/LoadingPage.vue";
import NotePageFrame from '../components/notes/views/NotePageFrame.vue';
import { storedApiGetNoteAndItsChildren, storedApiGetNoteWithDescendents } from "../storedApi";
import { viewType } from "../models/viewTypes";

export default {
  name: "NoteShowPage",
  props: { noteId: [String, Number], viewType: String },
  data() {
    return {
      notePosition: null,
      loading: true,
      pauseFetching: false,
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
    fetchData(loading) {
      this.loading = loading ?? true;
      const storedApiCall = this.viewTypeObj.fetchAll ?
                              storedApiGetNoteWithDescendents :
                              storedApiGetNoteAndItsChildren

      storedApiCall(this.$store, this.noteId)
      .then((res) => {
        this.notePosition = res.notePosition;
      }).finally(() => this.loading = false);
    },
    pollData() {
        if(!this.viewTypeObj.fetchAll) {
          if (this.pauseFetching){
            if (this.polling)
              this.pausePolling();
          } else {
            this.startPolling();
          }
        } else {
          this.fetchData(false);
        }      
      },
    onEditing(value){
      if (value==="onEditing"){
        this.pauseFetching = true;
        this.pausePolling();
      }
      else{
        this.pauseFetching = false;
        this.startPolling();
      }
      
    },
    startPolling() {
      this.polling=setInterval(() => {
        this.fetchData(false);
      }, 2000);
    },
    pausePolling() {
      clearInterval(this.polling);
    }
  },
  watch: {
    noteId() {
      this.fetchData(this.loading);
    },
    viewType() {
      this.fetchData(this.loading);
    },
  },
  mounted() {
    this.pollData();
  },
  unmounted() {
    clearInterval(this.polling);
  }
};

</script>