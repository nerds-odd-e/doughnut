<template>
  <LoadingPage v-bind="{ loading, contentExists: !!notePosition }">
    <div class="box">
      <div class="header">
        <NoteControl :noteId="highlightNoteId" :deleteRedirect="false"/>
        <Breadcrumb v-bind="notePosition" :noteRouteName="`mindmap`"/>
      </div>
      <div class="content">
        <NoteMindmapWithListner
          v-bind="{noteId, highlightNoteId}"
          @highlight="highlight"
        />
      </div>
    </div>
  </LoadingPage>
</template>

<script>
import LoadingPage from "./commons/LoadingPage.vue";
import NoteControl from "../components/commons/NoteControl.vue";
import { storedApiGetNoteWithDescendents } from "../storedApi";
import NoteMindmapWithListner from "../components/notes/NoteMindmapWithListner.vue";
import Breadcrumb from "../components/notes/Breadcrumb.vue";

export default {
  name: "NoteOverviewPage",
  props: { noteId: [String, Number] },
  data() {
    return {
      loading: false,
      notePosition: null,
      highlightNoteId: null,
    };
  },
  components: { NoteControl, LoadingPage, NoteMindmapWithListner, Breadcrumb },
  methods: {
    highlight(id) { this.highlightNoteId = id},
    fetchData() {
      this.loading = true;
      storedApiGetNoteWithDescendents(this.$store, this.noteId)
      .then((res) => {
        this.notePosition = res.notePosition;
      })
      .finally(() => this.loading = false)
      ;
    },
  },
  watch: {
    noteId() {
      this.highlightNoteId = this.noteId
      this.fetchData();
    },
  },
  mounted() {
    this.highlightNoteId = this.noteId
    this.fetchData();
  },
};
</script>

<style lang="sass" scoped>
.box
  display: flex
  flex-flow: column
  height: 100%

.box .header
  flex: 0 1 auto

.box .content
  flex: 1 1 auto
  overflow: hidden

.box .footer
  flex: 0 1 40px
</style>