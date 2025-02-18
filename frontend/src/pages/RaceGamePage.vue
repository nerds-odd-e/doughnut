<template>
  <ContainerPage v-bind="{ title: 'Car Dice Roll Race' }">
    <div class="race-game">
      <img 
        src="/src/assets/race/car-scar0.png" 
        alt="Racing car" 
        class="race-car"
      />
    </div>
  </ContainerPage>
</template>

<script setup lang="ts">
import ContainerPage from "./commons/ContainerPage.vue"
</script>

<style scoped>
.race-game {
  padding: 1rem;
  display: flex;
  justify-content: center;
}

.race-car {
  max-width: 400px;
  height: auto;
}
</style> 