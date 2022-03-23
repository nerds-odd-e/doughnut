<template>
  <LoadingPage v-bind="{ loading, contentExists: !!true }">
    this is your answer
  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import LoadingPage from "./commons/LoadingPage.vue";
import NoteSphereComponent from '../components/notes/views/NoteSphereComponent.vue';
import useStoredLoadingApi from "../managedApi/useStoredLoadingApi";

export default defineComponent({
  setup() {
    return useStoredLoadingApi({initalLoading: true});
  },
  name: "NoteShowPage",
  props: { answerId: Number },
  components: { LoadingPage, NoteSphereComponent },
  computed: {
  },
  methods: {
    fetchData() {
      // const storedApiCall = this.viewTypeObj.fetchAll ?
      //                         this.storedApi.getNoteWithDescendents :
      //                         this.storedApi.getNoteAndItsChildren

      // storedApiCall(this.noteId)
    },
  },
  watch: {
    answerId() {
      this.fetchData();
    },
  },
  mounted() {
    this.fetchData();
  },
});

</script>