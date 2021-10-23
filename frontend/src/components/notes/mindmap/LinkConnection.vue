<template>
  <g style="stroke:green;fill:green" v-if="connection">
    <line v-bind="connection"
      style="stroke-width:1" marker-end="url(#arrowhead)"  />
  </g>
</template>

<script>
export default {
  props: {
    link: Object,
    linkStart: Object,
    mindmap: Object,
  },
  computed: {
    connection() {
      return this.mindmap.linkToTargetNote(this.linkStart, this.link)
    },

  },
}


</script>
