<template>
      <line v-if="!mindmapSector.isHead" v-bind="mindmapSector.connection(150, 50, scale)"
        style="stroke:#FF6700;stroke-width:2" />
</template>

<script setup>
import { computed } from "@vue/reactivity";
import NoteTitleWithLink from "../NoteTitleWithLink.vue";
import MindmapSector from "@/models/MindmapSector";

const props = defineProps({
  note: Object,
  scale: Number,
  mindmapSector: MindmapSector,
  linkFragment: { type: Object, default: NoteTitleWithLink },
});

</script>

<style lang="sass" scoped>
</style>
