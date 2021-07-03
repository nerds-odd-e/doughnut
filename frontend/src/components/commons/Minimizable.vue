<template>
  <transition name="fade">
    <StickTopBar v-if="minimized">
        <slot name="minimizedContent"/>
    </StickTopBar>
  </transition>
  <slot v-if="!minimized" name="fullContent"/>
</template>

<script>
import StickTopBar from "./StickTopBar.vue"

export default {
  name: 'Minimizable',
  props: { minimized: Boolean },
  components: { StickTopBar },
}
</script>

<style scoped>
.fade-enter-active, .fade-leave-active {
  transition: opacity 2s ease;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
