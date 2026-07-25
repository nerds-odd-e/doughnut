<template>
  <GlobalBar />
  <div class="home-container">
    <LearningFlowSection
      :user-name="user?.name || 'To Doughnut'"
      :nav-items="upperNavItems"
    />

    <div class="lower-half">
      <div class="sharing-section">
        <nav class="nav-items">
          <NavigationItem
            v-for="item in lowerNavItems"
            :key="item.name"
            v-bind="item"
          />
        </nav>
      </div>
    </div>
  </div>
  <div class="cli-section">
    <h3>Doughnut CLI</h3>
    <pre class="cli-install-code"><code># Install (macOS, Linux, WSL)
curl https://doughnut.odd-e.com/install -fsS | bash
# Install (Windows PowerShell)
irm 'https://doughnut.odd-e.com/install?win32=true' | iex
# Run CLI
doughnut</code></pre>
  </div>
  <div class="ending">
    <p>Our goal of note taking is not to remember, but to forget.</p>
    <p>Doughnut will eventually become our digital twin, our shadow.</p>
    <p>It will think independently, for us, as us.</p>
  </div>
</template>

<script setup lang="ts">
import LearningFlowSection from "@/components/home/LearningFlowSection.vue"
import NavigationItem from "@/components/navigation/NavigationItem.vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import { useNavigationItems } from "@/composables/useNavigationItems"
import type { User } from "@generated/doughnut-backend-api"
import { inject, type Ref } from "vue"

const user = inject<Ref<User | undefined>>("currentUser")
const { upperNavItems, lowerNavItems } = useNavigationItems()
</script>

<style lang="scss" scoped>
.home-container {
  min-height: 200vh;
}

.nav-items {
  display: flex;
  justify-content: space-between;
  position: relative;
  z-index: 2;
}

.lower-half {
  min-height: 100vh;
  min-height: 100dvh;
  padding: 2rem;
  background: linear-gradient(
    to bottom,
    var(--color-base-200),
    var(--color-base-100)
  );
}

.sharing-section {
  display: flex;
  justify-content: center;
  padding: 2rem;
}

.cli-section {
  padding: 2rem;
  text-align: center;
}

.cli-install-code {
  text-align: left;
  display: inline-block;
  background: var(--color-base-200);
  padding: 1rem 1.5rem;
  border-radius: 8px;
  overflow-x: auto;
}

.ending {
  text-align: center;
  padding: 2rem;
}
</style>
