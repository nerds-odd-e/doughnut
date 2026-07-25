<template>
  <div class="daisy-tabs daisy-tabs-box bg-base-200 p-2 flex justify-center mb-4">
    <a
      :class="`daisy-tab daisy-tab-lg ${activePage === 'recentlyLearned' ? 'daisy-tab-active' : ''}`"
      role="button"
      href="#"
      @click.prevent="setActivePage('recentlyLearned')"
    >Recently Learned</a>
    <a
      :class="`daisy-tab daisy-tab-lg ${activePage === 'recentlyRecalled' ? 'daisy-tab-active' : ''}`"
      role="button"
      href="#"
      @click.prevent="setActivePage('recentlyRecalled')"
    >Recently Recalled</a>
  </div>

  <RecentlyLearnedNotes v-if="activePage === 'recentlyLearned'" />
  <RecentlyRecalledNotes v-if="activePage === 'recentlyRecalled'" />
</template>

<script setup lang="ts">
import { computed } from "vue"
import { useRoute, useRouter } from "vue-router"
import RecentlyLearnedNotes from "@/components/recent/RecentlyLearnedNotes.vue"
import RecentlyRecalledNotes from "@/components/recent/RecentlyRecalledNotes.vue"

type TabType = "recentlyLearned" | "recentlyRecalled"

const route = useRoute()
const router = useRouter()

const activePage = computed({
  get(): TabType {
    const tab = route.query.tab as string | undefined
    if (tab === "recentlyLearned" || tab === "recentlyRecalled") {
      return tab
    }
    return "recentlyLearned"
  },
  set(value: TabType) {
    router.push({
      name: "settingsRecent",
      query: { tab: value },
    })
  },
})

const setActivePage = (tab: TabType) => {
  activePage.value = tab
}
</script>
