<template>
  <div>
    <ContentLoader v-if="!circles" />
    <template v-else>
      <li class="list-group-item" v-for="circle in circles" :key="circle.id">
        <router-link
          :to="{ name: 'circleShow', params: { circleId: circle.id } }"
        >
          {{ circle.name }}
        </router-link>
      </li>
    </template>
    <div class="btn-group">
      <PopButton btn-class="btn btn-secondary" title="Create a new circle">
        <template #default="{ closer }">
          <CircleNewDialog @close-dialog="closer" />
        </template>
      </PopButton>
      <router-link btn-class="btn btn-primary" :to="{ name: 'circleJoin' }">
        Join a circle
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import CircleNewDialog from "@/components/circles/CircleNewDialog.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import type { Circle } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import { onMounted, ref } from "vue"

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
