<template>
  <ContainerPage
    v-bind="{ contentLoaded: true, title: `Recent Notes` }"
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
          :class="`nav-link ${activePage === 'dueForReview' ? 'active' : ''}`"
          role="button"
          href="#"
          @click="activePage = 'dueForReview'"
        >Due for Review</a>
      </li>
    </ul>

    <RecentlyAddedNotes v-if="activePage === 'recentlyAdded'" />
    <RecentlyLearnedNotes v-if="activePage === 'recentlyLearned'" />
    <DueForReviewNotes v-if="activePage === 'dueForReview'" />
  </ContainerPage>
</template>

<script setup lang="ts">
import { ref } from "vue"
import ContainerPage from "@/pages/commons/ContainerPage.vue"
import RecentlyAddedNotes from "@/components/recent/RecentlyAddedNotes.vue"
import RecentlyLearnedNotes from "@/components/recent/RecentlyLearnedNotes.vue"
import DueForReviewNotes from "@/components/recent/DueForReviewNotes.vue"

const activePage = ref(
  "recentlyAdded" as
    | "recentlyAdded"
    | "recentlyLearned"
    | "dueForReview"
    | undefined
)
</script>
