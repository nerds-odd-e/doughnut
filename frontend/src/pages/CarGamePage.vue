<template>
    <div class="daisy-min-h-screen daisy-w-full daisy-bg-gradient-to-br daisy-from-blue-50 daisy-to-indigo-100 daisy-relative daisy-overflow-hidden">
        <!-- Background decorative elements -->
        <div class="daisy-absolute daisy-top-0 daisy-left-0 daisy-w-full daisy-h-full daisy-pointer-events-none">
            <div class="daisy-absolute daisy-top-20 daisy-left-10 daisy-w-32 daisy-h-32 daisy-bg-yellow-200 daisy-rounded-full daisy-opacity-20 daisy-blur-xl"></div>
            <div class="daisy-absolute daisy-bottom-20 daisy-right-10 daisy-w-40 daisy-h-40 daisy-bg-pink-200 daisy-rounded-full daisy-opacity-20 daisy-blur-xl"></div>
            <div class="daisy-absolute daisy-top-1/2 daisy-left-1/4 daisy-w-24 daisy-h-24 daisy-bg-green-200 daisy-rounded-full daisy-opacity-20 daisy-blur-xl"></div>
        </div>

        <!-- Player Indicator (Top Right) -->
        <div class="daisy-absolute daisy-top-6 daisy-right-6 daisy-z-10">
            <div class="daisy-bg-white/90 daisy-backdrop-blur-sm daisy-px-6 daisy-py-3 daisy-rounded-2xl daisy-shadow-lg daisy-border daisy-border-white/20">
                <div class="daisy-flex daisy-items-center daisy-space-x-2">
                    <div class="daisy-w-3 daisy-h-3 daisy-bg-green-500 daisy-rounded-full daisy-animate-pulse"></div>
                    <span class="daisy-text-gray-800 daisy-font-semibold daisy-text-lg">Player {{ currentPlayer?.name ?? "" }}</span>
                </div>
            </div>
        </div>
        <div class="daisy-absolute daisy-top-6 daisy-left-6 daisy-z-10">
            <div class="daisy-bg-white/90 daisy-backdrop-blur-sm daisy-px-6 daisy-py-3 daisy-rounded-2xl daisy-shadow-lg daisy-border daisy-border-white/20">
                <div class="daisy-flex daisy-items-center daisy-space-x-2">
                    <div class="daisy-w-3 daisy-h-3 daisy-bg-green-500 daisy-rounded-full daisy-animate-pulse"></div>
                    <span id="number-of-players" class="daisy-text-gray-800 daisy-font-semibold daisy-text-lg">Number Of
                     Players: {{ listPlayers ? listPlayers.length : 0 }}
                    </span>
                </div>
            </div>
        </div>

        <!-- Main Game Board -->
        <div class="daisy-flex daisy-flex-col daisy-items-center daisy-justify-center daisy-min-h-screen daisy-p-8 daisy-relative daisy-z-10">
            <!-- Game Title -->
            <div class="daisy-text-center daisy-mb-12 daisy-animate-fade-in">
                <h1 class="daisy-text-6xl daisy-font-bold daisy-bg-gradient-to-r daisy-from-blue-600 daisy-to-purple-600 daisy-bg-clip-text daisy-text-transparent daisy-mb-4">
                    🏎️ Car Race
                </h1>
                <p class="daisy-text-xl daisy-text-gray-600 daisy-font-medium">Roll the dice and race to victory!</p>
            </div>

            <!-- Game Board Area -->
            <div class="daisy-bg-white/80 daisy-backdrop-blur-sm daisy-w-full daisy-max-w-6xl daisy-h-[500px] daisy-rounded-3xl daisy-relative daisy-mb-12 daisy-shadow-2xl daisy-border daisy-border-white/30 daisy-overflow-hidden">
                <!-- Racing track background -->
                <div class="daisy-absolute daisy-inset-0 daisy-bg-gradient-to-r daisy-from-green-200 daisy-to-green-300 daisy-opacity-30"></div>

                <!-- Track lines -->
                <div class="daisy-absolute daisy-top-1/2 daisy-left-0 daisy-w-full daisy-h-1 daisy-bg-white daisy-opacity-60 daisy-transform -daisy-translate-y-1/2"></div>
                <div class="daisy-absolute daisy-top-1/2 daisy-left-0 daisy-w-full daisy-h-1 daisy-bg-white daisy-opacity-40 daisy-transform -daisy-translate-y-1/2 daisy-mt-8"></div>
                <div class="daisy-absolute daisy-top-1/2 daisy-left-0 daisy-w-full daisy-h-1 daisy-bg-white daisy-opacity-40 daisy-transform -daisy-translate-y-1/2 -daisy-mt-8"></div>

                <!-- Cars on the left side -->
                <div class="daisy-absolute daisy-left-8 daisy-top-1/2 daisy-transform -daisy-translate-y-1/2 daisy-flex daisy-flex-col daisy-space-y-8">
                    <div v-for="(carScar, index) in carScars"
                         :key="carScar"
                         class="daisy-w-28 daisy-h-12 daisy-rounded-xl daisy-relative daisy-shadow-lg daisy-transform daisy-transition-all daisy-duration-300 hover:daisy-scale-105"
                         :class="getCarStyle(index)">
                        <img :src="carScar" alt="car scar" class="daisy-w-full daisy-h-full daisy-object-cover daisy-object-center daisy-rounded-xl" />
                          <!-- Car glow effect -->
                          <div class="daisy-absolute daisy-inset-0 daisy-bg-gradient-to-r daisy-from-yellow-400/20 daisy-to-orange-400/20 daisy-rounded-xl daisy-animate-pulse"></div>
                    </div>
                </div>

                <!-- Progress Bar/Track -->
                <div class="daisy-absolute daisy-bottom-12 daisy-left-1/2 daisy-transform -daisy-translate-x-1/2 daisy-w-[90%]">
                    <!-- Blue track line with gradient -->
                    <div class="daisy-h-3 daisy-bg-gradient-to-r daisy-from-blue-400 daisy-to-blue-600 daisy-rounded-full daisy-relative daisy-shadow-lg">
                        <!-- Progress marker with enhanced styling -->
                        <div class="daisy-absolute daisy-left-16 daisy-top-0 daisy-transform -daisy-translate-y-3">
                            <div class="daisy-w-0 daisy-h-0 daisy-border-l-6 daisy-border-r-6 daisy-border-b-6 daisy-border-l-transparent daisy-border-r-transparent daisy-border-b-yellow-400 daisy-drop-shadow-lg"></div>
                            <div class="player-position daisy-text-center daisy-mt-3 daisy-text-white daisy-font-bold daisy-text-lg daisy-bg-yellow-400 daisy-px-3 daisy-py-1 daisy-rounded-full daisy-shadow-lg"
                            :class="getMarkerPosition()">{{ totalSteps }}</div>
                        </div>

                        <!-- Track markers -->
                        <div class="daisy-absolute daisy-top-0 daisy-left-0 daisy-w-full daisy-h-full daisy-flex daisy-justify-between daisy-items-center daisy-px-4">
                            <div v-for="i in 10" :key="i" class="daisy-w-1 daisy-h-1 daisy-bg-white daisy-rounded-full daisy-opacity-60"></div>
                        </div>
                    </div>
                </div>

                <!-- Finish line -->
                <div class="daisy-absolute daisy-right-0 daisy-top-0 daisy-w-8 daisy-h-full daisy-bg-gradient-to-r daisy-from-transparent daisy-to-yellow-300 daisy-opacity-60"></div>
            </div>

            <!-- Dice Section -->
            <div class="daisy-flex daisy-flex-col daisy-items-center daisy-space-y-6 daisy-animate-bounce-in">
                <!-- Dice Result Display -->
                <div class="daisy-bg-white/90 daisy-backdrop-blur-sm daisy-w-32 daisy-h-20 daisy-rounded-2xl daisy-flex daisy-items-center daisy-justify-center daisy-shadow-xl daisy-border daisy-border-white/30 daisy-transform daisy-transition-all daisy-duration-500"
                     :class="diceRolling ? 'daisy-scale-110 daisy-rotate-12' : 'daisy-scale-100 daisy-rotate-0'">
                    <span id="dice-result-display" class="daisy-text-5xl daisy-font-bold daisy-bg-gradient-to-r daisy-from-purple-600 daisy-to-pink-600 daisy-bg-clip-text daisy-text-transparent">{{ diceResult }}</span>
                </div>
                <div class="btn-group">
                  <button
                    id="switch-mode-normal-btn"
                    @click.prevent="switchToNormal()"
                    class="daisy-font-bold daisy-py-4 daisy-px-10 daisy-rounded-2xl daisy-transition-all daisy-duration-300 daisy-shadow-lg hover:daisy-shadow-xl daisy-transform hover:daisy-scale-105 daisy-border daisy-border-white/20 daisy-relative daisy-overflow-hidden"
                    :class="{'daisy-bg-white': !isNormalMode, 'daisy-text-white daisy-bg-blue-500': isNormalMode}">
                    Normal
                  </button>
                  <button
                    id="switch-mode-super-btn"
                    @click.prevent="switchToSuper()"
                    class="daisy-font-bold daisy-py-4 daisy-px-10 daisy-rounded-2xl daisy-transition-all daisy-duration-300 daisy-shadow-lg hover:daisy-shadow-xl daisy-transform hover:daisy-scale-105 daisy-border daisy-border-white/20 daisy-relative daisy-overflow-hidden"
                    :class="{'daisy-bg-white': !isSuperMode, 'daisy-text-white daisy-bg-blue-500': isSuperMode}">
                    Super
                  </button>
                </div>
                <!-- Dice Button -->
                <button
                    id="roll-dice-button"
                    @click="rollDice"
                    :disabled="diceRolling"
                    class="daisy-bg-gradient-to-r daisy-from-blue-600 daisy-to-purple-600 hover:daisy-from-blue-700 hover:daisy-to-purple-700 daisy-text-white daisy-font-bold daisy-py-4 daisy-px-10 daisy-rounded-2xl daisy-transition-all daisy-duration-300 daisy-shadow-lg hover:daisy-shadow-xl daisy-transform hover:daisy-scale-105 daisy-border daisy-border-white/20 daisy-relative daisy-overflow-hidden"
                    :class="diceRolling ? 'daisy-opacity-50 daisy-cursor-not-allowed' : 'daisy-opacity-100'"
                >
                    <span class="daisy-relative daisy-z-10 daisy-flex daisy-items-center daisy-space-x-2">
                        <span>🎲</span>
                        <span>{{ diceRolling ? 'Rolling...' : 'Roll Dice' }}</span>
                    </span>
                    <!-- Button shine effect -->
                    <div class="daisy-absolute daisy-inset-0 daisy-bg-gradient-to-r daisy-from-transparent daisy-via-white/20 daisy-to-transparent daisy-transform -daisy-skew-x-12 daisy-translate-x-full daisy-transition-transform daisy-duration-1000 hover:daisy-translate-x-[-200%]"></div>
                </button>
            </div>

            <!-- Game Stats -->
            <div class="daisy-mt-12 daisy-flex daisy-space-x-8 daisy-animate-fade-in">
                <div class="daisy-bg-white/80 daisy-backdrop-blur-sm daisy-px-6 daisy-py-4 daisy-rounded-xl daisy-shadow-lg daisy-border daisy-border-white/30">
                    <div class="daisy-text-center">
                        <div class="daisy-text-2xl daisy-font-bold daisy-text-gray-800 current-round">{{ numberOfRounds }}</div>
                        <div class="daisy-text-sm daisy-text-gray-600">Rounds</div>
                    </div>
                </div>
                <div class="daisy-bg-white/80 daisy-backdrop-blur-sm daisy-px-6 daisy-py-4 daisy-rounded-xl daisy-shadow-lg daisy-border daisy-border-white/30">
                    <div class="daisy-text-center">
                        <div class="daisy-text-2xl daisy-font-bold daisy-text-gray-800 damage-position">{{ damage }}</div>
                        <div class="daisy-text-sm daisy-text-gray-600">Damage</div>
                    </div>
                </div>
                <div class="daisy-bg-white/80 daisy-backdrop-blur-sm daisy-px-6 daisy-py-4 daisy-rounded-xl daisy-shadow-lg daisy-border daisy-border-white/30">
                    <div class="daisy-text-center">
                        <div class="daisy-text-2xl daisy-font-bold daisy-text-gray-800">🏆</div>
                        <div class="daisy-text-sm daisy-text-gray-600">Best Score</div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from "vue"
import carScar0 from "@/assets/car-scar0.png"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { Players, Rounds } from "@generated/backend"

const { managedApi } = useLoadingApi()

const GAME_MODE = Object.freeze({
  NORMAL: "NORMAL",
  SUPER: "SUPER",
})

// Game state
const diceResult = ref(6)
const currentPlayer = ref<Players>()
const listPlayers = ref<Players[]>()
const diceRolling = ref(false)
const numberOfRounds = ref(0)
const totalSteps = ref(0)
const damage = ref(0)
const gameMode = ref<string>(GAME_MODE.NORMAL)
const MAX_STEPS = 20;

const isNormalMode = computed(() => gameMode.value === GAME_MODE.NORMAL)
const isSuperMode = computed(() => gameMode.value === GAME_MODE.SUPER)

// Dice rolling functionality
const rollDice = async () => {
  if (currentPlayer.value === undefined) return
  //TODO: call API to roll the dice
  if (diceRolling.value) return

  diceRolling.value = true

  // Animate dice rolling  // get result from API
  const result: Rounds = await managedApi.restGameController.rollDice(
    currentPlayer.value.id,
    gameMode.value
  )

  if (
    result === undefined ||
    result?.dice === undefined ||
    result?.dice < 1 ||
    result?.dice > 6
  )
    return

  // Final result
  diceResult.value = result.dice
  diceRolling.value = false

  numberOfRounds.value++
  if (isSuperMode.value && result.damage !== undefined) {
    damage.value = result.damage
  }
  totalSteps.value += (diceResult.value % 2 === 0 ? 2 : 1) - damage.value
}

// Car animation classes
const getCarStyle = (index: number) => {
  const animations = [
    "hover:daisy-animate-bounce",
    "hover:daisy-animate-pulse",
    "hover:daisy-animate-ping",
  ]
  return `${animations[index % animations.length]} car-position-${totalSteps.value%MAX_STEPS}`
}

const getMarkerPosition = () => {
  return `car-position-${totalSteps.value%MAX_STEPS}`
}

const carScars = [carScar0]
const fetchCarScars = async () => {
  const player = await managedApi.restGameController.joinGame()
  currentPlayer.value = player
}
const switchToSuper = () => {
  gameMode.value = GAME_MODE.SUPER
}
const switchToNormal = () => {
  gameMode.value = GAME_MODE.NORMAL
}

const fetchListPlayers = async () => {
  const playersList = await managedApi.restGameController.fetchPlayers()
  listPlayers.value = playersList
}

onMounted(async () => {
  await fetchCarScars()
  await fetchListPlayers()
})
</script>

<style>
@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes bounce-in {
  0% {
    opacity: 0;
    transform: scale(0.3);
  }
  50% {
    opacity: 1;
    transform: scale(1.05);
  }
  70% {
    transform: scale(0.9);
  }
  100% {
    opacity: 1;
    transform: scale(1);
  }
}

.daisy-animate-fade-in {
  animation: fade-in 0.8s ease-out;
}

.daisy-animate-bounce-in {
  animation: bounce-in 1s ease-out;
}

.car-position-1 {
  transform: translateX(1vw) !important;
}

.car-position-2 {
  transform: translateX(4vw) !important;
}

.car-position-3 {
  transform: translateX(7vw) !important;
}

.car-position-4 {
  transform: translateX(10vw) !important;
}

.car-position-5 {
  transform: translateX(13vw) !important;
}

.car-position-6 {
  transform: translateX(16vw) !important;
}

.car-position-7 {
  transform: translateX(19vw) !important;
}

.car-position-8 {
  transform: translateX(22vw) !important;
}

.car-position-9 {
  transform: translateX(25vw) !important;
}

.car-position-10 {
  transform: translateX(28vw) !important;
}

.car-position-11 {
  transform: translateX(31vw) !important;
}

.car-position-12 {
  transform: translateX(34vw) !important;
}

.car-position-13 {
  transform: translateX(37vw) !important;
}

.car-position-14 {
  transform: translateX(40vw) !important;
}

.car-position-15 {
  transform: translateX(43vw) !important;
}

.car-position-16 {
  transform: translateX(46vw) !important;
}

.car-position-17 {
  transform: translateX(49vw) !important;
}

.car-position-18 {
  transform: translateX(52vw) !important;
}

.car-position-19 {
  transform: translateX(55vw) !important;
}

.car-position-20 {
  transform: translateX(58vw) !important;
}

</style>
