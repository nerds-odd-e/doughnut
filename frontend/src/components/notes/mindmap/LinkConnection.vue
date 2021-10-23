<template>
  <g class="notes-link" style="stroke:green;fill:none" v-if="path">
    <path 
      :d="path"
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
    path() {
      return this.mindmap.linkToTargetNote(this.linkStart, this.link)
    },

  },
}


</script>
