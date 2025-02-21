<template>
  <ContainerPage v-bind="{ title: 'Car Dice Roll Race' }">
    <div class="race-game">
      <div v-if="isLoading && initLoad" class="loading-state">
        Loading game...
      </div>
      <template v-else>
        <div class="player-join">
          <input 
            type="text" 
            class="daisy-input daisy-input-bordered" 
            aria-label="name"
            placeholder="Enter your name"
            v-model="playerName"
          />
          <button class="daisy-btn daisy-btn-primary" @click="handleJoin">JOIN</button>
        </div>

        <span>{{ gameProgress?.displayName ?? 'Rahul' }}</span>
        <div class="race-track">
          <img 
            src="/src/assets/race/car-scarhp6.png" 
            alt="Racing car" 
            class="race-car"
            :style="{ left: `${(gameProgress?.carPosition ?? 0) * (100 / maxPosition)}%` }"
          />
          <div class="track"></div>
          <div class="max-position">{{ maxPosition }}</div>
        </div>
        
        <table class="daisy-table">
          <thead>
            <tr>
              <th>Car Position</th>
              <th>Round Count</th>
              <th>Last Dice Face</th>
              <th>Car HP</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td id="car-position">{{ gameProgress?.carPosition ?? 0 }}</td>
              <td id="round-count">{{ gameProgress?.roundCount ?? 0 }}</td>
              <td id="last-dice-face">{{ gameProgress?.lastDiceFace ?? '-' }}</td>
              <td id="car-hp">{{ gameProgress?.carHp ?? 6 }}</td>
            </tr>
          </tbody>
        </table>

        <div class="button-container">
          <button class="daisy-btn daisy-btn-outline" @click="handleGoNormal">GO NORMAL</button>
          <button class="daisy-btn daisy-btn-outline" @click="handleGoSuper">GO SUPER</button>
          <button class="daisy-btn daisy-btn-outline" @click="handleReset">RESET</button>
        </div>
      </template>
    </div>
  </ContainerPage>
</template>

<script setup lang="ts">
import ContainerPage from "./commons/ContainerPage.vue"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { ref, onMounted } from "vue"
import type { CurrentProgressDTO } from "@/generated/backend"

const { managedApi } = useLoadingApi()
const maxPosition = ref(20)

// Get or create playerId
const getStoredPlayerId = (): string => {
  const stored = localStorage.getItem("raceGamePlayerId")
  if (stored) return stored

  const newId = crypto.randomUUID()
  localStorage.setItem("raceGamePlayerId", newId)
  return newId
}

const playerId = ref(getStoredPlayerId())
const isLoading = ref(true)
const initLoad = ref(true)
const gameProgress = ref<CurrentProgressDTO>()
const playerName = ref("")

const handleJoin = () => {
  // Implementation will be added later
  console.log("Join clicked with name:", playerName.value)
}

const fetchProgress = async () => {
  isLoading.value = true
  try {
    const response = await managedApi.raceGameController.getCurrentProgress(
      playerId.value
    )
    gameProgress.value = response.currentProgress
  } catch (error: unknown) {
    console.error("Failed to fetch progress:", error)
  } finally {
    isLoading.value = false
    initLoad.value = false
  }
}

const handleGoNormal = async () => {
  try {
    await managedApi.raceGameController.rollDiceNormal({
      playerId: playerId.value,
    })
    // Fetch the updated progress after rolling the dice
    await fetchProgress()
  } catch (error: unknown) {
    console.error("Failed to roll dice:", error)
  }
}

const handleGoSuper = async () => {
  try {
    await managedApi.raceGameController.rollDiceSuper({
      playerId: playerId.value,
    })
    // Fetch the updated progress after rolling the dice
    await fetchProgress()
  } catch (error: unknown) {
    console.error("Failed to roll dice in super mode:", error)
  }
}

const handleReset = async () => {
  try {
    await managedApi.raceGameController.resetGame({
      playerId: playerId.value,
    })
    // Fetch the updated progress after reset
    await fetchProgress()
  } catch (error: unknown) {
    console.error("Failed to reset game:", error)
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

.player-join {
  display: flex;
  gap: 1rem;
  align-items: center;
  margin-bottom: 1rem;
}

.race-track {
  position: relative;
  width: 100%;
  max-width: 800px;
  height: 120px;
  display: flex;
  align-items: flex-end;
}

.track {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 8px;
  background-color: #333;
  border-radius: 4px;
}

.race-car {
  position: absolute;
  width: 120px;
  height: auto;
  z-index: 1;
  margin-bottom: 4px;
  transition: left 0.5s ease-out;
  margin-left: -120px;
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

.max-position {
  position: absolute;
  bottom: -25px;
  right: 0;
  font-weight: bold;
  color: #333;
}

.loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
  font-size: 1.2rem;
  color: #666;
}
</style>