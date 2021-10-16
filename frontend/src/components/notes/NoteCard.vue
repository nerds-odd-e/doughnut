<template>
  <div class="note-card" role="card" :aria-label="note.title" :style="`top:${y}vh; left:${x}vw`">
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
import NoteTitleWithLink from "./NoteTitleWithLink.vue";
const props = defineProps({
  note: Object,
  offset: Number,
  linkFragment: { type: Object, default: NoteTitleWithLink },
});

const x = computed(()=>{return 45 + props.offset})
const y = computed(()=>{return 39})
</script>

<style lang="sass" scoped>
.note-card
  position: absolute
  width: 150px
  min-height: 50px
  background-color: white
.note-card-title
  font-size: 1rem
.note-card-body
  font-size: 0.8rem

</style>
