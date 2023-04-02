<template>
  <div class="thin-loading-bar">
    <slot />
  </div>
</template>

<script>
export default {};
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
