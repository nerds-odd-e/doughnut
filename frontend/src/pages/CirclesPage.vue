<template>
  <ContainerPage title="My Circles"
    v-bind="{ contentLoaded: true }"
   >
    <ContentLoader v-if="!circles" />
    <template v-else>
      <ul class="list-group">
        <li class="list-group-item" v-for="circle in circles" :key="circle.id">
          <router-link
            :to="{ name: 'circleShow', params: { circleId: circle.id } }"
          >
            {{ circle.name }}
          </router-link>
        </li>
      </ul>
    </template>
    <div class="btn-group mt-3">
      <PopButton btn-class="btn btn-secondary" title="Create a new circle">
        <template #default="{ closer }">
          <CircleNewDialog @close-dialog="closer" />
        </template>
      </PopButton>
      <router-link class="btn btn-primary" :to="{ name: 'circleJoin' }">
        Join a circle
      </router-link>
    </div>
  </ContainerPage>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import type { Circle } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import CircleNewDialog from "@/components/circles/CircleNewDialog.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import ContainerPage from "@/pages/commons/ContainerPage.vue"

const { managedApi } = useLoadingApi()
const circles = ref<Circle[] | undefined>(undefined)

const fetchData = () => {
  managedApi.restCircleController.index().then((res) => {
    circles.value = res
  })
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.list-group {
  margin-bottom: 1rem;
}
</style>
