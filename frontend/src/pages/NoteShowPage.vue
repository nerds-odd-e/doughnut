<template>
  <LoadingPage v-bind="{ loading, contentExists: !!notePosition, inContainer: true }">
    <NotePageFrame
      v-bind="{
        noteId,
        notePosition,
        deleteRedirect: true,
        expandChildren: true,
        noteRouteName: 'noteShow',
        noteComponent: 'NoteWithChildrenCards'}"/>

    <router-link
      :to="{ name: 'noteOverview', params: { noteId: noteId } }"
      role="button"
      class="btn btn-sm"
    >
      Full view mode
    </router-link>

    <router-link
      :to="{ name: 'mindmap', params: { noteId: noteId } }"
      role="button"
      class="btn btn-sm"
    >
      Mindmap mode
    </router-link>

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
  props: { noteId: [String, Number], viewType: String },
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