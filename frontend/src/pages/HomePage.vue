<template>
  <div class="home-container">
    <div class="upper-half" ref="upperHalf">
      <h1 class="welcome-text">Welcome {{ user?.name || 'To Doughnut' }}!</h1>

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
              <div v-for="(item, index) in upperNavItems"
                   :key="item.name"
                   class="step-container"
              >
                <div class="step-content">
                  <div class="nav-item-wrapper">
                    <NavigationItem
                      v-bind="item"
                      :class="{ 'will-fly': isScrolling }"
                      class="nav-item"
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

    <div class="lower-half" ref="lowerHalf">
      <div class="sharing-section">
        <nav class="nav-items">
          <NavigationItem
            v-for="item in lowerNavItems"
            :key="item.name"
            v-bind="item"
            :class="{ 'will-fly': isScrolling }"
          />
        </nav>
      </div>
    </div>
  </div>
  <div class="ending">
    <p>Our goal of note taking is not to remember, but to forget.</p>
    <p>Doughnut will eventually become our digital twin, our shadow.</p>
    <p>It will think independently, for us, as us.</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, type Ref, inject } from "vue"
import type { User } from "generated/backend"
import { useNavigationItems } from "@/composables/useNavigationItems"
import NavigationItem from "@/components/navigation/NavigationItem.vue"

const user = inject<Ref<User | undefined>>("currentUser")
const { upperNavItems, lowerNavItems } = useNavigationItems()

const upperHalf = ref<HTMLElement>()
const lowerHalf = ref<HTMLElement>()
const isScrolling = ref(false)

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

const handleScroll = () => {
  if (!upperHalf.value) return

  const upperRect = upperHalf.value.getBoundingClientRect()
  const scrollPosition = window.scrollY
  const threshold = upperRect.height * 0.7

  isScrolling.value = scrollPosition > threshold
}

const updateFlowPath = () => {
  const svg = document.querySelector(".flow-background") as SVGElement
  const iconWrappers = document.querySelectorAll(
    ".nav-item-wrapper"
  ) as NodeListOf<HTMLElement>
  if (!svg || iconWrappers.length !== 3) return

  const svgRect = svg.getBoundingClientRect()
  const positions = Array.from(iconWrappers).map((wrapper) => {
    const rect = wrapper.getBoundingClientRect()
    return {
      x: rect.left + rect.width / 2 - svgRect.left,
      y: rect.top + rect.height / 2 - svgRect.top,
    }
  })

  const path = document.querySelector(".flow-path") as SVGPathElement
  if (!path) return

  const spacing = 30
  const curveRadius = 20
  const arrowSize = 8
  const wrapPadding = 80
  const edgeMargin = 60
  const sideMargin = 60
  const leftOffset = 60
  const connectionGap = 40
  const iconWidth = 70

  if (positions[0] && positions[1] && positions[2]) {
    const pathData = `
      M ${positions[0].x + connectionGap + leftOffset - iconWidth},${positions[0].y}
      H ${positions[1].x - connectionGap + leftOffset - iconWidth}
      M ${positions[1].x + connectionGap + leftOffset - iconWidth},${positions[1].y}
      H ${positions[2].x - connectionGap + leftOffset - iconWidth}
      M ${positions[2].x + leftOffset},${positions[2].y}
      h ${wrapPadding}
      q ${curveRadius} 0 ${curveRadius} ${curveRadius}
      v ${svgRect.height - positions[2].y - edgeMargin}
      q 0 ${curveRadius} -${curveRadius} ${curveRadius}
      h -${svgRect.width - sideMargin * 2}
      q -${curveRadius} 0 -${curveRadius} -${curveRadius}
      v -${svgRect.height - positions[0].y - edgeMargin}
      q 0 -${curveRadius} ${curveRadius} -${curveRadius}
      H ${positions[0].x - spacing + leftOffset - iconWidth}
    `
    path.setAttribute("d", pathData)

    const arrowPositions = [
      {
        x: positions[1].x - connectionGap + leftOffset - iconWidth,
        y: positions[1].y,
      },
      {
        x: positions[2].x - connectionGap + leftOffset - iconWidth,
        y: positions[2].y,
      },
      {
        x: positions[0].x - spacing + leftOffset - iconWidth,
        y: positions[0].y,
      },
    ]

    const arrowHeads = document.querySelectorAll(
      ".arrow-marker"
    ) as NodeListOf<SVGPathElement>
    arrowHeads.forEach((arrow, index) => {
      const pos = arrowPositions[index]
      if (pos) {
        arrow.setAttribute(
          "d",
          `
          M ${pos.x - arrowSize},${pos.y - arrowSize / 2}
          L ${pos.x},${pos.y}
          L ${pos.x - arrowSize},${pos.y + arrowSize / 2}
        `
        )
      }
    })
  }
}

onMounted(() => {
  window.addEventListener("scroll", handleScroll)
  window.addEventListener("resize", updateFlowPath)
  setTimeout(updateFlowPath, 100)
})

onUnmounted(() => {
  window.removeEventListener("scroll", handleScroll)
  window.removeEventListener("resize", updateFlowPath)
})
</script>

<style lang="scss" scoped>
.home-container {
  min-height: 200vh;
}

.upper-half {
  min-height: 100vh;
  padding: 2rem;
  background: linear-gradient(to bottom, #ffffff, #f5f5f5);
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

.nav-items {
  display: flex;
  justify-content: space-between;
  position: relative;
  z-index: 2;
}

.nav-item {
  flex: 0 0 auto;
  margin: 0 1rem;
}

.flow-arrows {
  position: absolute;
  top: 50%;
  left: 0;
  width: 100%;
  height: 50px;
  transform: translateY(-50%);
  z-index: 1;
}

.will-fly {
  transition: transform 0.3s ease-out;
  transform: translateX(-100px);
}

.cards-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 2rem;
  padding: 2rem;
}

.note-card {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.lower-half {
  min-height: 100vh;
  padding: 2rem;
  background: linear-gradient(to bottom, #f5f5f5, #ffffff);
}

.sharing-section {
  display: flex;
  justify-content: center;
  padding: 2rem;
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
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
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

.flow-arrows {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: none;
}

.globe-icon,
.lightbulb-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  font-size: 2rem;
}

.icons-flow {
  position: relative;
  width: 100%;
  height: 150px;
  margin-bottom: 2rem;
}

.icons-row {
  display: flex;
  justify-content: space-between;
  padding: 0 200px;
  position: relative;
  z-index: 2;
}

.icon-position {
  display: flex;
  flex-direction: column;
  align-items: center;
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
  .learning-process {
    flex-direction: column;
  }

  .nav-items {
    flex-direction: column;
    align-items: center;
  }

  .flow-arrows {
    display: none;
  }

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

  .flow-arrows {
    display: none;
  }

  .icons-row {
    flex-direction: column;
    align-items: center;
    gap: 2rem;
    padding: 0;
  }

  .flow-background {
    display: none;
  }
}

.ending {
  text-align: center;
  padding: 2rem;
}
</style>
