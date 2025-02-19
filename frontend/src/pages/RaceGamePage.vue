<template>
  <ContainerPage v-bind="{ title: 'Car Dice Roll Race' }">
    <div class="race-game">
      <img src="/src/assets/race/car-scar0.png" alt="Racing car" class="race-car" />
      
      <table class="daisy-table">
        <thead>
          <tr>
            <th>Car Position</th>
            <th>Round Count</th>
            <th>Last Dice Face</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td id="car-position">{{ gameProgress?.carPosition ?? 0 }}</td>
            <td id="round-count">{{ gameProgress?.roundCount ?? 0 }}</td>
            <td id="last-dice-face">{{ gameProgress?.lastDiceFace ?? '-' }}</td>
          </tr>
        </tbody>
      </table>

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
import { ref, onMounted } from "vue"
import type { CurrentProgressDTO } from "@/generated/backend"

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
const gameProgress = ref<CurrentProgressDTO>()

const fetchProgress = async () => {
  try {
    const response = await managedApi.raceGameController.getCurrentProgress(
      playerId.value
    )
    gameProgress.value = response.currentProgress
  } catch (error: unknown) {
    console.error("Failed to fetch progress:", error)
  }
}

const handleGoNormal = async () => {
  try {
    await managedApi.raceGameController.rollDice({
      playerId: playerId.value,
    })
    // Fetch the updated progress after rolling the dice
    await fetchProgress()
  } catch (error: unknown) {
    console.error("Failed to roll dice:", error)
  }
}

onMounted(() => {
  fetchProgress()
})
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

.daisy-table {
  background: white;
  border-radius: 0.5rem;
  overflow: hidden;
}

.daisy-table th,
.daisy-table td {
  padding: 1rem;
  text-align: center;
}

.daisy-table th {
  background: rgba(0, 0, 0, 0.1);
}
</style>