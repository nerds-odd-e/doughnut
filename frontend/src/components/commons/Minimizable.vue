<template>
  <div class="stick-top-bar">
  <transition name="mini">
    <div v-if="minimized" class="container">
        <slot name="minimizedContent"/>
    </div>
  </transition>
  </div>
  <transition name="max">
    <div v-if="!minimized">
      here it is:
      <slot v-if="!minimized" name="fullContent"/>
    </div>
  </transition>

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

.max-enter-active, .max-leave-active {
  transition: all .5s ease;
}

.max-enter-from, .max-leave-to {
  transform: translateY(-80vh);
  opacity: 0.8;
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
