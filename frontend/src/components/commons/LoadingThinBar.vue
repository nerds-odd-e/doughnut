<template>
  <ToolbarOver class="loading-bar">
    <ContentLoader :height="15" :speed="2" primary-color="#888"></ContentLoader>
  </ToolbarOver>
</template>

<script>
import { ContentLoader } from "vue-content-loader"
import ToolbarOver from "./ToolbarOver.vue"

export default {
  components: {
    ContentLoader,
    ToolbarOver,
  },
}
</script>
