<template>
  <LoadingPage v-bind="{ loading, contentExists: !!notePosition }">
    <div class="box" v-if="!loading">
      <div class="header">
        <Breadcrumb v-bind="notePosition" />
      </div>
      <div class="content">
        <div class="mindmap">
          <NoteMindmap v-bind="{ noteId }" />
        </div>
      </div>
    </div>
  </LoadingPage>
</template>

<script>
import LoadingPage from "./commons/LoadingPage.vue";
import { storedApiGetNoteWithDescendents } from "../storedApi";
import NoteMindmap from "../components/notes/NoteMindmap.vue";
import Breadcrumb from "../components/notes/Breadcrumb.vue";

export default {
  name: "NoteOverviewPage",
  props: { noteId: Number },
  data() {
    return {
      notePosition: null,
      loading: true,
    };
  },
  components: { LoadingPage, NoteMindmap, Breadcrumb },
  methods: {

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
      this.fetchData();
    },
  },
  mounted() {
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
  background-color: green

.box .footer
  flex: 0 1 40px

.mindmap
  position: relative
  top: 50%
  left: 50%
</style>