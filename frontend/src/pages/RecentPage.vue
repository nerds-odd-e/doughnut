<template>
  <ContainerPage
    v-bind="{ contentLoaded: true, title: `Recent` }"
  >
    <div class="daisy-tabs daisy-tabs-boxed daisy-bg-base-200 daisy-p-2 daisy-flex daisy-justify-center daisy-mb-4">
      <a
        :class="`daisy-tab daisy-tab-lg ${activePage === 'recentlyAdded' ? 'daisy-tab-active' : ''}`"
        role="button"
        href="#"
        @click="activePage = 'recentlyAdded'"
      >Recently Added/Updated</a>
      <a
        :class="`daisy-tab daisy-tab-lg ${activePage === 'recentlyLearned' ? 'daisy-tab-active' : ''}`"
        role="button"
        href="#"
        @click="activePage = 'recentlyLearned'"
      >Recently Learned</a>
      <a
        :class="`daisy-tab daisy-tab-lg ${activePage === 'recentlyReviewed' ? 'daisy-tab-active' : ''}`"
        role="button"
        href="#"
        @click="activePage = 'recentlyReviewed'"
      >Recently Reviewed</a>
    </div>

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
