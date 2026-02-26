<template>
  <Teleport to="body">
    <div
      class="overlay"
      :class="[
        {
          'overlay--centered': centered,
          'overlay--dark': dark,
          'overlay--opaque': opaque,
        },
        $attrs.class,
      ]"
      :style="{ zIndex }"
    >
      <slot />
    </div>
  </Teleport>
</template>

<script setup lang="ts">
defineOptions({ inheritAttrs: false })
withDefaults(
  defineProps<{
    centered?: boolean
    dark?: boolean
    opaque?: boolean
    zIndex?: number
  }>(),
  {
    centered: false,
    dark: false,
    opaque: false,
    zIndex: 9990,
  }
)
</script>

<style scoped>
.overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: table;
  transition: opacity 0.3s ease;
}

.overlay--centered {
  display: flex;
  align-items: center;
  justify-content: center;
}

.overlay--dark {
  background-color: rgba(0, 0, 0, 0.7);
}

.overlay--opaque {
  background-color: rgba(0, 0, 0, 1);
}
</style>
