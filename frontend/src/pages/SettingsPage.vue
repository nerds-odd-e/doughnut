<template>
  <ContainerPage v-bind="{ contentLoaded: true, title: 'Settings' }">
    <div
      class="daisy-tabs daisy-tabs-box bg-base-200 p-2 flex justify-center mb-4"
    >
      <router-link
        v-for="tab in tabs"
        :key="tab.name"
        :to="{ name: tab.name }"
        :class="`daisy-tab daisy-tab-lg ${isActive(tab.name) ? 'daisy-tab-active' : ''}`"
      >
        {{ tab.label }}
      </router-link>
    </div>
    <RouterView />
  </ContainerPage>
</template>

<script setup lang="ts">
import ContainerPage from "@/pages/commons/ContainerPage.vue"
import { useRoute, RouterView } from "vue-router"

const route = useRoute()

const tabs = [
  { name: "settingsGeneral", label: "General" },
  { name: "settingsRecent", label: "Recent" },
  { name: "settingsAccessTokens", label: "Access Tokens" },
] as const

const isActive = (name: string) => route.name === name
</script>
