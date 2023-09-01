<template>
  <DragListner class="mindmap-event-receiver" v-model="offset">
    <div class="mindmap">
      <NoteMindmap
        v-bind="{
          noteId,
          noteRealms,
          offset,
          storageAccessor,
        }"
      />
    </div>
    <div class="mindmap-info" @click.prevent="reset">
      <span class="scale">{{ scalePercentage }}</span>
      <span class="offset">{{ offsetMsg }}</span>
      <span class="offset">{{ rotateMsg }}&deg;</span>
    </div>
  </DragListner>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import NoteMindmap from "../mindmap/NoteMindmap.vue";
import DragListner from "../../commons/DragListner.vue";
import { NoteRealmsReader } from "../../../store/NoteRealmCache";
import { StorageAccessor } from "../../../store/createNoteStorage";

const defaultOffset = { x: 0, y: 0, scale: 1.0, rotate: 0 };

export default defineComponent({
  props: {
    noteId: { type: Number, required: true },
    noteRealms: {
      type: Object as PropType<NoteRealmsReader>,
      required: true,
    },
    storageAccessor: {
      type: Object as PropType<StorageAccessor>,
      required: true,
    },
  },
  data() {
    return {
      offset: { ...defaultOffset },
    };
  },
  components: { NoteMindmap, DragListner },
  methods: {
    reset() {
      this.offset = { ...defaultOffset };
    },
  },
  computed: {
    centerX() {
      return `calc(50% + ${this.offset.x}px)`;
    },
    centerY() {
      return `calc(50% + ${this.offset.y}px)`;
    },
    scalePercentage() {
      return `${(this.offset.scale * 100).toFixed(0)}%`;
    },
    offsetMsg() {
      return `offset: (${this.offset.x.toFixed(0)}, ${this.offset.y.toFixed(
        0,
      )})`;
    },
    rotateMsg() {
      return `rotate: ${((this.offset.rotate * 180) / Math.PI).toFixed(0)}`;
    },
  },
});
</script>

<style lang="sass" scoped>

.mindmap
  position: relative
  top: v-bind("centerY")
  left: v-bind("centerX")
  width: 1px
  height: 1px

.mindmap-event-receiver
  position: relative
  background-color: azure
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
