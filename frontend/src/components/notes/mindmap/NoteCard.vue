<template>
  <div class="note-card" role="card" :aria-label="note.title" :style="`top:${y}px; left:${x}px`">
    <h5 class="note-card-title">
      <component :is="linkFragment" :note="note" class="card-title" />
    </h5>
    <div class="note-card-body">
      <p>{{ note.shortDescription }}</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from "@vue/reactivity";
import NoteTitleWithLink from "../NoteTitleWithLink.vue";
import MindmapSector from "@/models/MindmapSector";

const props = defineProps({
  note: Object,
  mindmapSector: MindmapSector,
  linkFragment: { type: Object, default: NoteTitleWithLink },
});

const x = computed(()=>props.mindmapSector.x - 150 / 2)
const y = computed(()=>props.mindmapSector.y - 50 / 2)
</script>

<style lang="sass" scoped>
.note-card
  z-index: 2000
  position: absolute
  width: 150px
  min-height: 50px
  background-color: white
.note-card-title
  font-size: 1rem
.note-card-body
  font-size: 0.8rem

</style>
