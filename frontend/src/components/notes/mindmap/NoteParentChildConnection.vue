<template>

  <g class="parent-child">
      <line v-if="!mindmapSector.isHead" v-bind="connection"
        style="stroke-width:15" marker-end="url(#treearrow)"  />
  </g>
</template>

<script>
export default {

  props: {
    note: Object,
    mindmapSector: Object,
    mindmap: Object,
  },
  computed: {
    connection() { return this.mindmap.connectFromParent(this.mindmapSector)},
  },
}
</script>