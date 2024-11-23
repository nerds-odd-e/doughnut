<template>
  <div class="sidebar-container">
    <div class="scrolling-body">
      <nav class="navbar navbar-expand-lg navbar-dark bg-dark">
        <BrandBar />
      </nav>
      <ul class="list-group">
        <li v-if="user?.admin" class="list-group-item">
          <router-link :to="{ name: 'adminDashboard' }">
            Admin Dashboard
          </router-link>
        </li>
        <li class="list-group-item">
          <ReviewButton class="btn" />
        </li>
        <li class="list-group-item">
          <router-link :to="{ name: 'notebooks' }"> My Notebooks </router-link>
        </li>
        <li class="list-group-item">
          <router-link :to="{ name: 'recent' }" class="d-flex align-items-center gap-2">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-clock-history" viewBox="0 0 16 16">
              <path d="M8.515 1.019A7 7 0 0 0 8 1V0a8 8 0 0 1 .589.022zm2.004.45a7 7 0 0 0-.985-.299l.219-.976c.383.086.76.2 1.126.342zm1.37.71a7 7 0 0 0-.439-.27l.493-.87a8 8 0 0 1 .979.654l-.615.789a7 7 0 0 0-.418-.302zm1.834 1.79a7 7 0 0 0-.653-.796l.724-.69c.27.285.52.59.747.91l-.818.576zm.744 1.352a7 7 0 0 0-.214-.468l.893-.45a8 8 0 0 1 .45 1.088l-.95.313a7 7 0 0 0-.179-.483m.53 2.507a7 7 0 0 0-.1-1.025l.985-.17c.067.386.106.778.116 1.17l-1 .025zm-.131 1.538c.033-.17.06-.339.081-.51l.993.123a8 8 0 0 1-.23 1.155l-.964-.267c.046-.165.086-.332.12-.501m-.952 2.379c.184-.29.346-.594.486-.908l.914.405c-.16.36-.345.706-.555 1.038l-.845-.535m-.964 1.205c.122-.122.239-.248.35-.378l.758.653a8 8 0 0 1-.401.432l-.707-.707z"/>
              <path d="M8 1a7 7 0 1 0 4.95 11.95l.707.707A8.001 8.001 0 1 1 8 0z"/>
              <path d="M7.5 3a.5.5 0 0 1 .5.5v5.21l3.248 1.856a.5.5 0 0 1-.496.868l-3.5-2A.5.5 0 0 1 7 9V3.5a.5.5 0 0 1 .5-.5"/>
            </svg>
            Recent Notes
          </router-link>
        </li>
        <li class="list-group-item">
          <router-link :to="{ name: 'bazaar' }"> Bazaar </router-link>
        </li>
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
      </ul>
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
    <nav class="navbar justify-content-between fixed-bottom-bar bg-white">
      <UserActionsButton
        v-bind="{ user }"
        @update-user="$emit('updateUser', $event)"
      />
    </nav>
  </div>
</template>

<script setup lang="ts">
import CircleNewDialog from "@/components/circles/CircleNewDialog.vue"
import ContentLoader from "@/components/commons/ContentLoader.vue"
import PopButton from "@/components/commons/Popups/PopButton.vue"
import type { Circle, User } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import type { PropType } from "vue"
import { onMounted, ref } from "vue"
import BrandBar from "./BrandBar.vue"
import ReviewButton from "./ReviewButton.vue"
import UserActionsButton from "./UserActionsButton.vue"

const { managedApi } = useLoadingApi()

defineProps({
  user: { type: Object as PropType<User>, required: true },
})

defineEmits(["updateUser"])

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

<style lang="scss" scoped>
.sidebar-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.scrolling-body {
  flex: 1;
  overflow-y: auto;
}

.fixed-bottom-bar {
  position: sticky;
  bottom: 0;
  width: 100%;
  height: 60px; /* Adjust this value to match the height of the fixed-bottom-bar */
}
</style>
