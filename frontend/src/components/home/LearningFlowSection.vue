<template>
  <div class="upper-half" ref="upperHalf">
    <h1 class="welcome-text">Welcome {{ userName }}!</h1>

    <div class="learning-process">
      <div class="flow-container">
        <div class="learning-flow-group">
          <svg class="flow-background" preserveAspectRatio="none">
            <rect width="100%" height="100%" fill="none" />
            <g class="arrows" fill="none" stroke="rgba(0,0,0,0.6)" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
              <path class="flow-path"/>
              <path class="arrow-marker" />
              <path class="arrow-marker" />
              <path class="arrow-marker" />
            </g>
          </svg>

          <div class="learning-steps">
            <div v-for="(item, index) in navItems"
                 :key="item.name"
                 class="step-container"
            >
              <div class="step-content">
                <div class="nav-item-wrapper">
                  <NavigationItem
                    v-bind="item"
                    :class="{ 'will-fly': isScrolling }"
                    class="nav-item"
                    @goToNextAssimilation="goToNextAssimilation"
                  />
                </div>
                <div class="note-card">
                  <h3>{{ cardTitles[index] }}</h3>
                  <p>{{ cardDescriptions[index] }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import NavigationItem from "@/components/navigation/NavigationItem.vue"
import { useGoToNextAssimilation } from "@/composables/useGoToNextAssimilation"
import { useLearningFlowPath } from "@/composables/useLearningFlowPath"
import type { Component } from "vue"
import { ref } from "vue"

const { goToNextAssimilation } = useGoToNextAssimilation()

defineProps<{
  userName: string
  navItems: Array<{
    name?: string
    label: string
    icon: Component
    isActive: boolean
    badge?: number
    badgeClass?: string
    hasDropdown?: boolean
    nonClickable?: boolean
  }>
}>()

const upperHalf = ref<HTMLElement>()
const { isScrolling } = useLearningFlowPath(upperHalf)

const cardTitles = [
  "Taking notes is only the beginning",
  "Assimilate to become truely yours",
  "Recall is better than review",
]

const cardDescriptions = [
  "Capture what you saw, what you heard, what you read, your thoughts and ideas, with the help of AI tools. Your notebooks learn as you learn.",
  "Start to understand your notes, and tracking your memories about them. Decide what to keep and what to let go.",
  "Using AI generated questions to help you recall the notes to keep your memories alive. Improve the notes stucture continously as we learn.",
]
</script>

<style lang="scss" scoped>
.upper-half {
  min-height: 100vh;
  min-height: 100dvh;
  padding: 2rem;
  background: linear-gradient(
    to bottom,
    var(--color-base-100),
    var(--color-base-200)
  );
}

.welcome-text {
  text-align: center;
  margin-bottom: 3rem;
  font-size: 2rem;
  font-weight: 600;
}

.learning-process {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2rem;
  margin-bottom: 3rem;
  position: relative;
  padding: 2rem 0;
}

.flow-container {
  flex: 1;
  position: relative;
  max-width: 800px;
}

.nav-item {
  flex: 0 0 auto;
  margin: 0 1rem;
}

.will-fly {
  transition: transform 0.3s ease-out;
  transform: translateX(-100px);
}

.learning-steps {
  display: flex;
  justify-content: space-between;
  position: relative;
  z-index: 2;
  margin: 0 100px;
  padding: 2rem 0;
}

.step-container {
  flex: 0 1 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.step-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  width: 100%;
}

.nav-item-wrapper {
  transform: scale(1.5);
  margin-bottom: 1rem;
}

.note-card {
  width: 100%;
  background: var(--color-base-100);
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 4px color-mix(in oklch, var(--color-base-content) 10%, transparent);
  min-height: 200px;
  margin-top: 1rem;
  display: flex;
  flex-direction: column;

  h3 {
    margin-bottom: 1rem;
    font-size: 1.1rem;
  }

  p {
    flex: 1;
  }
}

.learning-flow-group {
  position: relative;
  width: 100%;
  padding: 2rem 0;
}

.flow-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  overflow: visible;

  .arrows {
    filter: drop-shadow(0 0 8px rgba(0, 0, 0, 0.3));

    path {
      stroke-linecap: round;
      stroke-linejoin: round;
      stroke-dasharray: 10 5;
      filter: drop-shadow(0 0 3px rgba(0, 0, 0, 0.5));
    }

    .flow-path {
      animation: glowPulse 1.5s ease-in-out infinite;
    }

    .arrow-marker {
      animation: glowPulse 1.5s ease-in-out infinite;
    }
  }
}

@keyframes glowPulse {
  0% {
    filter: drop-shadow(0 0 2px rgba(0, 0, 0, 0.4));
    stroke: rgba(0, 0, 0, 0.6);
  }
  50% {
    filter: drop-shadow(0 0 8px rgba(0, 0, 0, 0.6));
    stroke: rgba(0, 0, 0, 0.8);
  }
  100% {
    filter: drop-shadow(0 0 2px rgba(0, 0, 0, 0.4));
    stroke: rgba(0, 0, 0, 0.6);
  }
}

@media (max-width: 768px) {
  .learning-steps {
    flex-direction: column;
    align-items: center;
    gap: 3rem;
    margin: 0 20px;
  }

  .step-container {
    width: 100%;
    max-width: 300px;
  }

  .nav-item-wrapper {
    transform: scale(1.2);
  }

  .flow-background {
    display: none;
  }
}
</style>
