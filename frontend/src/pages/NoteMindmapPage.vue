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
        <div class="mindmap-event-receiver"
         @mousedown="startDrag"
         @touchstart="startDrag"
         @mousemove="onDrag"
         @touchmove="onDrag"
         @mouseup="stopDrag"
         @touchend="stopDrag"
        />
      </div>
    </div>
  </LoadingPage>
</template>

<script>
import LoadingPage from "./commons/LoadingPage.vue";
import { storedApiGetNoteWithDescendents } from "../storedApi";
import NoteMindmap from "../components/notes/mindmap/NoteMindmap.vue";
import Breadcrumb from "../components/notes/Breadcrumb.vue";

export default {
  name: "NoteOverviewPage",
  props: { noteId: Number },
  data() {
    return {
      notePosition: null,
      loading: true,
      dragging: false,
      start: {},
      offset: {x: 0, y: 0},
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

    startDrag(e) {
      e = e.changedTouches ? e.changedTouches[0] : e;
      this.dragging = true;
      this.start.x = e.clientX;
      this.start.y = e.clientY;
      this.start.offset = {...this.offset}
    },
    onDrag(e) {
      if (this.dragging) {
        e = e.changedTouches ? e.changedTouches[0] : e;
        this.offset.x = this.start.offset.x + e.clientX - this.start.x;
        this.offset.y = this.start.offset.y + e.clientY - this.start.y;
      }
    },
    stopDrag() {
      this.dragging = false;
    }
  },
  computed: {
    centerX() {
      return `calc(50% + ${this.offset.x}px)`
    },
    centerY() {
      return `calc(50% + ${this.offset.y}px)`
    }
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
  overflow: hidden


.box .footer
  flex: 0 1 40px

.mindmap
  position: relative
  top: v-bind("centerY")
  left: v-bind("centerX")
  width: 1px
  height: 1px

.mindmap-event-receiver
  position: relative
  top: 0
  left: 0
  width: 100%
  height: 100%
  z-index: 1000

</style>