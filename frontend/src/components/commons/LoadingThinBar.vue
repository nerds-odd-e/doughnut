<template>
  <div class="toolbar-overlap loading-bar">
    <ContentLoader :height="15" :speed="2" primary-color="#888"></ContentLoader>
  </div>
</template>

<script setup lang="ts">
import { ContentLoader } from "vue-content-loader"
</script>

<style lang="scss" scoped>
.toolbar-overlap {
  position: fixed;
  left: 0;
  top: 0;
  width: 100%;
  height: 5px;
  z-index: 2147483647;

  svg {
    width: 100%;
    height: 100%;
    z-index: 2147483647;
  }
}
</style>
