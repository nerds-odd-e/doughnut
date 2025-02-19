<template>
  <ContainerPage v-bind="{ title: 'Car Dice Roll Race' }">
    <div class="race-game">
      <img src="/src/assets/race/car-scar0.png" alt="Racing car" class="race-car" />
      <div class="button-container">
        <button class="daisy-btn daisy-btn-outline" @click="handleGoNormal">GO NORMAL</button>
        <button class="daisy-btn daisy-btn-outline">GO SUPER</button>
        <button class="daisy-btn daisy-btn-outline">RESET</button>
      </div>
    </div>
  </ContainerPage>
</template>

<script setup lang="ts">
import ContainerPage from "./commons/ContainerPage.vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { ref } from "vue"

const { managedApi } = useLoadingApi()

// Get or create playerId
const getStoredPlayerId = (): string => {
  const stored = localStorage.getItem("raceGamePlayerId")
  if (stored) return stored

  const newId = crypto.randomUUID()
  localStorage.setItem("raceGamePlayerId", newId)
  return newId
}

const playerId = ref(getStoredPlayerId())

const handleGoNormal = async () => {
  try {
    await managedApi.raceGameController.rollDice({
      playerId: playerId.value,
    })
  } catch (error: unknown) {
    console.error("Failed to roll dice:", error)
  }
}
</script>

<style scoped>
.race-game {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2rem;
}

.race-car {
  max-width: 400px;
  height: auto;
}

.button-container {
  display: flex;
  gap: 1rem;
  justify-content: center;
}
</style>