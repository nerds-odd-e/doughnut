<template>
  <ContainerPage v-bind="{ title: 'Car Dice Roll Race' }">
    <div class="race-game">
      <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.</p>
    </div>
  </ContainerPage>
</template>

<script setup lang="ts">
import ContainerPage from "./commons/ContainerPage.vue"
</script>

<style scoped>
.race-game {
  padding: 1rem;
}
</style> 