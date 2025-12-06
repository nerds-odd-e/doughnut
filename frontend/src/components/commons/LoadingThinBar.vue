<template>
  <div class="toolbar-overlap loading-bar"></div>
</template>

<script setup lang="ts">
</script>

<style lang="scss" scoped>
.toolbar-overlap {
  position: fixed;
  left: 0;
  bottom: 0;
  right: 0;
  width: 100vw;
  height: 1px;
  z-index: 2147483647;
  margin: 0;
  padding: 0;
  background-color: #888;
}
</style>
