<template>
    <div class="daisy-min-h-screen daisy-w-full daisy-bg-white">
        <!-- Player Indicator (Top Right) -->
        <div class="daisy-absolute daisy-top-4 daisy-right-4">
            <div class="daisy-bg-gray-200 daisy-px-4 daisy-py-2 daisy-rounded-lg">
                <span class="daisy-text-black daisy-font-medium">Player 1</span>
            </div>
        </div>

        <!-- Main Game Board -->
        <div class="daisy-flex daisy-flex-col daisy-items-center daisy-justify-center daisy-min-h-screen daisy-p-8">
            <!-- Game Board Area -->
            <div class="daisy-bg-gray-200 daisy-w-full daisy-max-w-4xl daisy-h-96 daisy-rounded-lg daisy-relative daisy-mb-8">
                <!-- Cars on the left side -->
                <div class="daisy-absolute daisy-left-4 daisy-top-1/2 daisy-transform -daisy-translate-y-1/2 daisy-flex daisy-flex-col daisy-space-y-4">
                    <div class="daisy-w-16 daisy-h-8 daisy-bg-red-600 daisy-rounded-lg daisy-relative">
                        <!-- Car details -->
                        <div class="daisy-absolute daisy-top-1 daisy-left-1 daisy-w-2 daisy-h-2 daisy-bg-black daisy-rounded-full"></div>
                        <div class="daisy-absolute daisy-top-1 daisy-right-1 daisy-w-2 daisy-h-2 daisy-bg-black daisy-rounded-full"></div>
                        <div class="daisy-absolute daisy-bottom-1 daisy-left-2 daisy-w-1 daisy-h-1 daisy-bg-gray-400 daisy-rounded-full"></div>
                        <div class="daisy-absolute daisy-bottom-1 daisy-right-2 daisy-w-1 daisy-h-1 daisy-bg-gray-400 daisy-rounded-full"></div>
                    </div>
                    <div class="daisy-w-16 daisy-h-8 daisy-bg-red-600 daisy-rounded-lg daisy-relative">
                        <div class="daisy-absolute daisy-top-1 daisy-left-1 daisy-w-2 daisy-h-2 daisy-bg-black daisy-rounded-full"></div>
                        <div class="daisy-absolute daisy-top-1 daisy-right-1 daisy-w-2 daisy-h-2 daisy-bg-black daisy-rounded-full"></div>
                        <div class="daisy-absolute daisy-bottom-1 daisy-left-2 daisy-w-1 daisy-h-1 daisy-bg-gray-400 daisy-rounded-full"></div>
                        <div class="daisy-absolute daisy-bottom-1 daisy-right-2 daisy-w-1 daisy-h-1 daisy-bg-gray-400 daisy-rounded-full"></div>
                    </div>
                    <div class="daisy-w-16 daisy-h-8 daisy-bg-red-600 daisy-rounded-lg daisy-relative">
                        <div class="daisy-absolute daisy-top-1 daisy-left-1 daisy-w-2 daisy-h-2 daisy-bg-black daisy-rounded-full"></div>
                        <div class="daisy-absolute daisy-top-1 daisy-right-1 daisy-w-2 daisy-h-2 daisy-bg-black daisy-rounded-full"></div>
                        <div class="daisy-absolute daisy-bottom-1 daisy-left-2 daisy-w-1 daisy-h-1 daisy-bg-gray-400 daisy-rounded-full"></div>
                        <div class="daisy-absolute daisy-bottom-1 daisy-right-2 daisy-w-1 daisy-h-1 daisy-bg-gray-400 daisy-rounded-full"></div>
                    </div>
                </div>

                <!-- Progress Bar/Track -->
                <div class="daisy-absolute daisy-bottom-8 daisy-left-1/2 daisy-transform -daisy-translate-x-1/2 daisy-w-3/4">
                    <!-- Blue track line -->
                    <div class="daisy-h-1 daisy-bg-blue-500 daisy-rounded-full daisy-relative">
                        <!-- Progress marker -->
                        <div class="daisy-absolute daisy-left-8 daisy-top-0 daisy-transform -daisy-translate-y-2">
                            <div class="daisy-w-0 daisy-h-0 daisy-border-l-4 daisy-border-r-4 daisy-border-b-4 daisy-border-l-transparent daisy-border-r-transparent daisy-border-b-gray-400"></div>
                            <div class="daisy-text-center daisy-mt-2 daisy-text-black daisy-font-bold">6</div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Dice Section -->
            <div class="daisy-flex daisy-flex-col daisy-items-center daisy-space-y-4">
                <!-- Dice Result Display -->
                <div class="daisy-bg-gray-200 daisy-w-24 daisy-h-16 daisy-rounded-full daisy-flex daisy-items-center daisy-justify-center">
                    <span class="daisy-text-4xl daisy-font-bold daisy-text-black">{{ diceResult }}</span>
                </div>
                
                <!-- Dice Button -->
                <button 
                    @click="rollDice"
                    class="daisy-bg-blue-600 hover:daisy-bg-blue-700 daisy-text-white daisy-font-bold daisy-py-3 daisy-px-8 daisy-rounded-lg daisy-transition-colors"
                >
                    Dice
                </button>
            </div>
        </div>
    </div>
</template>
  
<script setup lang="ts">
import { ref } from "vue"

// Game state
const diceResult = ref(6)
const currentPlayer = ref(1)

// Dice rolling functionality
const rollDice = () => {
  // Generate random number between 1 and 6
  diceResult.value = Math.floor(Math.random() * 6) + 1
}
</script>
  
  