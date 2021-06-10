<script>

import { defineAsyncComponent, defineComponent, ref } from "vue"
import NoteStatistics from "./NoteStatistics.vue"

export default defineComponent({
  props: { noteid: Number },
  components: {     
      NoteStatistics
    },
  setup() {
      const children = ref(null)

      const toggleStatistics = () => {
        children.value = 'NoteStatistics'
      }
return {
        toggleStatistics,
        children        
      }
    },
  })
</script>

<template>
  <a @click="toggleStatistics()" class="btn btn-sm" role="button" title="statistics">
  Statistics
  </a>
  <div>
     <component :is="children" :noteid="noteid"></component>
  </div>
</template>
