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
