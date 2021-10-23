<template>
  <g class="notes-link" style="stroke:green;fill:green" v-if="connection">
    <path 
      :d="`M ${connection.x1} ${connection.y1} ${connection.x2} ${connection.y2}`"
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
