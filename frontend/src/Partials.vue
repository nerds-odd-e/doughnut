<template>
  <a @click="toggleStatistics()" class="btn btn-sm" th:href="@{/links/{id}/link(id=${note.id})}" role="button" title="statistics">
  Statistics
  </a>
  <div>
     <component :is="children"></component>
  </div>
</template>

<script>

import { defineAsyncComponent, defineComponent, ref } from "vue"

export default defineComponent({
components: {     
      NoteStatistics: defineAsyncComponent(() => import("./components/NoteStatistics.vue"))
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