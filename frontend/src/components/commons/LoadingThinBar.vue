<template>
  <div class="thin-loading-bar loading-bar">
    <ContentLoader :height="15" :speed="2" primary-color="#888"></ContentLoader>
  </div>
</template>

<script>
import { ContentLoader } from "vue-content-loader";

export default {
  components: {
    ContentLoader,
  },
};
</script>
<style lang="scss" scoped>
.thin-loading-bar {
  position: absolute;
  left: 0;
  width: 100%;
  opacity: 0.5;
  z-index: 999999;

  svg {
    width: 100%;
    height: 100%;
    z-index: 999999;
  }
}
</style>
