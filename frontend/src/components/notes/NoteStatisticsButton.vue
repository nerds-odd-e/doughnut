<script>
import NoteStatistics from "./NoteStatistics.vue"

export default {
  name: 'NoteStatisticsButton',
  props: { noteid: Number, linkid: Number },
  components: { NoteStatistics },
  data() {
    return { statisticComponentName: null }
  },
  methods: {
    toggleStatistics() {
      if(!this.statisticComponentName) {
        this.statisticComponentName = 'NoteStatistics'
      }
      else {
        this.statisticComponentName = null
      }
    }
  },
  watch: {
    noteid() {
      this.statisticComponentName = null
    },
    linkid() {
      this.statisticComponentName = null
    }
  }
}
</script>

<template>
  <a @click="toggleStatistics()" class="btn btn-sm" role="button" title="statistics">
  Statistics
  </a>
  <div>
     <component :is="statisticComponentName" :noteid="noteid" :linkid="linkid"></component>
  </div>
</template>
