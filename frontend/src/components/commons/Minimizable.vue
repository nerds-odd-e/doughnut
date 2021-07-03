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

<style lang="scss">
.fade-enter-active, .fade-leave-active {
  transition: all .5s ease;
}

.fade-enter-from, .fade-leave-to {
  transform: translateY(60vh);
  opacity: 0;
}
</style>
