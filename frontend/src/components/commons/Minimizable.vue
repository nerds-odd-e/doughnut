<template>
  <slot name="minimizedContent" />
  <transition name="max">
    <div v-if="!minimized">
      <slot v-if="!minimized" name="fullContent" />
    </div>
  </transition>
</template>

<script>
export default {
  name: "Minimizable",
  props: {
    minimized: Boolean,
  },
};
</script>

<style lang="scss">
.mini-enter-active,
.mini-leave-active {
  transition: all 0.3s ease;
}

.mini-enter-from,
.mini-leave-to {
  transform: translateY(60vh);
  opacity: 0;
}

.max-enter-active,
.max-leave-active {
  transition: all 0.3s ease;
}

.max-enter-from,
.max-leave-to {
  transform: translateY(-80vh);
  opacity: 0.8;
}

</style>
