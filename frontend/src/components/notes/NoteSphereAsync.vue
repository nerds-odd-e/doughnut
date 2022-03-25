<template>
  <LoadingPage v-bind="{ loading, contentExists: true }">
    <NoteSphereComponent
      v-bind="{
        noteId,
        viewType,
        expandChildren,
      }"
      :key="noteId"

      />

  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import NoteSphereComponent from './views/NoteSphereComponent.vue';
import { ViewType, viewType } from "../../models/viewTypes";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";
import LoadingPage from '../../pages/commons/LoadingPage.vue';

export default defineComponent({
  setup() {
    return useStoredLoadingApi({initalLoading: true});
  },
  props: {
     noteId: { type: Number, required: true },
     viewType: String,
     expandChildren: { type: Boolean, required: true },
  },
  components: { LoadingPage, NoteSphereComponent },
  computed: {
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