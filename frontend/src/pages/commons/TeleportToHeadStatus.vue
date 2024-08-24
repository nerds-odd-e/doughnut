<template>
  <teleport v-if="teleportTargetExists" :to="teleportTargetName">
    <slot />
  </teleport>
</template>

<script setup lang="ts">
const teleportTargetName = "#head-status"
const teleportTargetExists = !!document.querySelector(teleportTargetName)
</script>
