<template>
  <div class="stick-top-bar">
  <transition name="mini">
    <div v-if="minimized" class="container">
        <slot name="minimizedContent"/>
    </div>
  </transition>
  </div>
  <slot v-if="!minimized" name="fullContent"/>
</template>

<script>

export default {
  name: 'Minimizable',
  props: { minimized: Boolean },
}
</script>

<style lang="scss">
.mini-enter-active, .mini-leave-active {
  transition: all .5s ease;
}

.mini-enter-from, .mini-leave-to {
  transform: translateY(60vh);
  opacity: 0;
}
.stick-top-bar {
  overflow: visible;
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 1000;
}

</style>
