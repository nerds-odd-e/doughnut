<template>
  <ContainerPage title="My Circles"
    v-bind="{ contentLoaded: true }"
   >
    <ContentLoader v-if="!circles" />
    <template v-else>
      <ul class="daisy-menu bg-base-200 w-full">
        <li v-for="circle in circles" :key="circle.id">
          <router-link
            :to="{ name: 'circleShow', params: { circleId: circle.id } }"
          >
            {{ circle.name }}
          </router-link>
        </li>
      </ul>
    </template>
    <div class="daisy-join mt-3">
      <PopButton btn-class="daisy-btn" title="Create a new circle">
        <template #default="{ closer }">
          <CircleNewDialog @close-dialog="closer" />
        </template>
      </PopButton>
      <router-link class="daisy-btn daisy-btn-primary" :to="{ name: 'circleJoin' }">
        Join a circle
      </router-link>
    </div>
  </ContainerPage>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import type { Circle } from "@generated/backend"
import { CircleController } from "@generated/backend/sdk.gen"
import {} from "@/managedApi/clientSetup"
import CircleNewDialog from "@/components/circles/CircleNewDialog.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import ContainerPage from "@/pages/commons/ContainerPage.vue"

const circles = ref<Circle[] | undefined>(undefined)

const fetchData = async () => {
  const { data: circlesList, error } = await CircleController.index({})
  if (!error) {
    // circlesList is guaranteed to be Circle[] when error is undefined
    circles.value = circlesList!
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.daisy-menu {
  margin-bottom: 1rem;
}
</style>
