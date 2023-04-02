<template>
  <ToolbarCover class="loading-bar">
    <ContentLoader :height="15" :speed="2" primary-color="#888"></ContentLoader>
  </ToolbarCover>
</template>

<script>
import { ContentLoader } from "vue-content-loader";
import ToolbarCover from "./ToolbarOver.vue";

export default {
  components: {
    ContentLoader,
    ToolbarCover,
  },
};
</script>
