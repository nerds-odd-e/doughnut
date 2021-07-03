<template>
    <div class="stick-top-bar">
      <div class="container">
        <slot />
      </div>
    </div>
</template>

<script>
export default {
}
</script>

<style scoped>
.stick-top-bar {
  overflow: hidden;
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 1000;
}
</style>
