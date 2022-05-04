<template>
  <LoadingPage v-bind="{ loading, contentExists: true }">
    <NoteRealm
      v-bind="{
        noteId,
        viewType,
        expandChildren,
        comments,
      }"
      :key="noteId"
    />
  </LoadingPage>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import NoteRealm from "./views/NoteRealm.vue";
import { ViewType, viewType } from "../../models/viewTypes";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";
import LoadingPage from "../../pages/commons/LoadingPage.vue";

export default defineComponent({
  setup() {
    return useStoredLoadingApi({ initalLoading: true });
  },
  props: {
    noteId: { type: Number, required: true },
    viewType: String,
    expandChildren: { type: Boolean, required: true },
  },
  components: { LoadingPage, NoteRealm },
  data() {
    return {
      comments: [] as Generated.Comment[],
    };
  },
  computed: {
    viewTypeObj(): ViewType {
      return viewType(this.viewType);
    },
  },
  methods: {
    async fetchComments() {
      this.comments = await this.api.comments.getNoteComments(this.noteId);
    },
    fetchData() {
      const storedApiCall = this.viewTypeObj.fetchAll
        ? this.storedApi.getNoteWithDescendents
        : this.storedApi.getNoteAndItsChildren;

      storedApiCall(this.noteId);
      this.fetchComments();
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
