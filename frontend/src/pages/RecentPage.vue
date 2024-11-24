<template>
  <ContainerPage
    v-bind="{ contentLoaded: true, title: `Recent` }"
  >
    <ul class="nav nav-tabs">
      <li class="nav-item">
        <a
          :class="`nav-link ${activePage === 'recentlyAdded' ? 'active' : ''}`"
          role="button"
          href="#"
          @click="activePage = 'recentlyAdded'"
        >Recently Added/Updated</a>
      </li>
      <li class="nav-item">
        <a
          :class="`nav-link ${activePage === 'recentlyLearned' ? 'active' : ''}`"
          role="button"
          href="#"
          @click="activePage = 'recentlyLearned'"
        >Recently Learned</a>
      </li>
      <li class="nav-item">
        <a
          :class="`nav-link ${activePage === 'recentlyReviewed' ? 'active' : ''}`"
          role="button"
          href="#"
          @click="activePage = 'recentlyReviewed'"
        >Recently Reviewed</a>
      </li>
    </ul>

    <RecentlyAddedNotes v-if="activePage === 'recentlyAdded'" />
    <RecentlyLearnedNotes v-if="activePage === 'recentlyLearned'" />
    <RecentlyReviewedNotes v-if="activePage === 'recentlyReviewed'" />
  </ContainerPage>
</template>

<script setup lang="ts">
import { ref } from "vue"
import ContainerPage from "@/pages/commons/ContainerPage.vue"
import RecentlyAddedNotes from "@/components/recent/RecentlyAddedNotes.vue"
import RecentlyLearnedNotes from "@/components/recent/RecentlyLearnedNotes.vue"
import RecentlyReviewedNotes from "@/components/recent/RecentlyReviewedNotes.vue"

const activePage = ref(
  "recentlyAdded" as
    | "recentlyAdded"
    | "recentlyLearned"
    | "recentlyReviewed"
    | undefined
)
</script>
