<template>
  <div class="toolbar-overlap">
    <slot />
  </div>
</template>

<script>
export default {};
</script>

<style lang="scss" scoped>
.toolbar-overlap {
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
