<template>
  <div
    v-if="!isMdOrLarger && opened"
    class="sidebar-drawer-backdrop fixed inset-x-0 bottom-0 bg-black/50 z-30"
    @click="opened = false"
  />
  <aside
    v-bind="$attrs"
    :class="
      isMdOrLarger
        ? opened
          ? 'relative'
          : 'hidden'
        : [
            'sidebar-drawer fixed left-0 z-40',
            opened ? 'translate-x-0' : '-translate-x-full',
          ]
    "
  >
    <slot />
  </aside>
</template>

<script setup lang="ts">
defineOptions({ inheritAttrs: false })

const opened = defineModel<boolean>("opened", { required: true })

defineProps<{ isMdOrLarger: boolean }>()
</script>

<style scoped lang="scss">
@use "@/assets/menu-variables.scss" as *;

.sidebar-drawer,
.sidebar-drawer-backdrop {
  top: calc(#{$main-menu-height-mobile} + env(safe-area-inset-top, 0px));
}

.sidebar-drawer {
  bottom: 0;
}

aside {
  max-height: 100%;
}
</style>
