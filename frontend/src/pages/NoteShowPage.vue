<template>
  <LoadingPage v-bind="{ loading, contentExists: !!notePosition, inContainer }">
    <NotePageFrame
      v-bind="{
        noteId,
        notePosition,
        deleteRedirect,
        viewType,
        expandChildren: true,
        noteRouteName: 'noteCards',
        noteComponent}"/>

    <NoteStatisticsButton :noteId="noteId" />
  </LoadingPage>
</template>

<script>
import LoadingPage from "./commons/LoadingPage.vue";
import NotePageFrame from '../components/notes/NotePageFrame.vue';
import NoteStatisticsButton from "../components/notes/NoteStatisticsButton.vue";
import { storedApiGetNoteAndItsChildren, storedApiGetNoteWithDescendents } from "../storedApi";

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
    noteComponent() {
      if(this.viewType === 'article') return 'NoteOverview'
      if(this.viewType === 'mindmap') return 'NoteMindmapWithListner'
      return 'NoteWithChildrenCards'
    },

    storedApiCall() {
      if(this.viewType === 'article') return storedApiGetNoteWithDescendents
      if(this.viewType === 'mindmap') return storedApiGetNoteWithDescendents
      return storedApiGetNoteAndItsChildren
    },

    deleteRedirect() {
      if(this.viewType === 'article') return false
      if(this.viewType === 'mindmap') return false
      return true
    },

    inContainer() {
      if(this.viewType === 'mindmap') return false
      return true
    },

  },
  methods: {
    fetchData() {
      this.loading = true
      this.storedApiCall(this.$store, this.noteId)
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