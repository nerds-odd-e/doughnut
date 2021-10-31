<template>
  <LoadingPage v-bind="{ loading, contentExists: !!notePosition }">
    <div class="box">
      <div class="header">
        <NoteControl :noteId="highlightNoteId" :deleteRedirect="false"/>
        <Breadcrumb v-bind="notePosition" :noteRouteName="`mindmap`"/>
      </div>
      <div class="content">
        <DragListner class="mindmap-event-receiver" v-model="offset">
          <div class="mindmap">
            <NoteMindmap v-bind="{
                highlightNoteId,
                noteId,
                scale: offset.scale,
                rotate: offset.rotate,
                ancestors: notePosition.ancestors }"
                @highlight="highlight"
            />
          </div>
        <div class="mindmap-info" @click.prevent="reset">
          <span class="scale">{{scalePercentage}}</span>
          <span class="offset">{{offsetMsg}}</span>
          <span class="offset">{{rotateMsg}}&deg;</span>
        </div>
        </DragListner>
      </div>
    </div>
  </LoadingPage>
</template>

<script>
import LoadingPage from "./commons/LoadingPage.vue";
import NoteControl from "../components/commons/NoteControl.vue";
import { storedApiGetNoteWithDescendents } from "../storedApi";
import NoteMindmap from "../components/notes/mindmap/NoteMindmap.vue";
import DragListner from "../components/commons/DragListner.vue";
import Breadcrumb from "../components/notes/Breadcrumb.vue";

const defaultOffset = {x: 0, y: 0, scale: 1.0, rotate: 0}

export default {
  name: "NoteOverviewPage",
  props: { noteId: [String, Number] },
  data() {
    return {
      loading: false,
      notePosition: null,
      offset: { ... defaultOffset },
      highlightNoteId: null,
    };
  },
  components: { NoteControl, LoadingPage, NoteMindmap, DragListner, Breadcrumb },
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
    reset() {
      this.offset = { ... defaultOffset }
    }
  },
  computed: {
    centerX() {
      return `calc(50% + ${this.offset.x}px)`
    },
    centerY() {
      return `calc(50% + ${this.offset.y}px)`
    },
    scalePercentage() {
      return `${(this.offset.scale * 100).toFixed(0)}%`
    },
    offsetMsg() {
      return `offset: (${this.offset.x.toFixed(0)}, ${this.offset.y.toFixed(0)})`
    },
    rotateMsg() {
      return `rotate: ${ (this.offset.rotate * 180 / Math.PI).toFixed(0)}`
    }
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
  z-index: 999

.mindmap-info
  position: relative
  display: inline
  top: calc(100% - 50px)
  left: 10px
  padding: 3px
  border-radius: 5px
  background-color: rgba(125, 125, 125, 0.5)
  font-size: 70%
  .offset
    margin-left: 10px

</style>