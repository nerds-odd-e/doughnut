<template>
  <div v-if="userPage">
    <table class="daisy-table daisy-table-zebra">
      <thead>
        <tr>
          <th>Username</th>
          <th>Notes</th>
          <th>Memory Trackers</th>
          <th>Last Note Time</th>
          <th>Last Assimilation Time</th>
          <th>Last Recall Time</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="user in userPage.users" :key="user.id">
          <td>{{ user.name }}</td>
          <td>{{ user.noteCount }}</td>
          <td>{{ user.memoryTrackerCount }}</td>
          <td>{{ formatTime(user.lastNoteTime) }}</td>
          <td>{{ formatTime(user.lastAssimilationTime) }}</td>
          <td>{{ formatTime(user.lastRecallTime) }}</td>
        </tr>
      </tbody>
    </table>

    <div
      v-if="userPage.totalPages && userPage.totalPages > 1"
      class="daisy-join daisy-mt-4"
    >
      <button
        class="daisy-join-item daisy-btn"
        :disabled="pageIndex === 0"
        @click="goToPage(pageIndex - 1)"
      >
        «
      </button>
      <button class="daisy-join-item daisy-btn">
        Page {{ pageIndex + 1 }} of {{ userPage.totalPages }}
      </button>
      <button
        class="daisy-join-item daisy-btn"
        :disabled="pageIndex >= (userPage.totalPages ?? 1) - 1"
        @click="goToPage(pageIndex + 1)"
      >
        »
      </button>
    </div>
  </div>
  <div v-else>Loading users...</div>
</template>

<script lang="ts" setup>
import { onMounted, ref } from "vue"
import { AdminUserController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import type { UserListingPage } from "@generated/doughnut-backend-api"

const userPage = ref<UserListingPage | undefined>(undefined)
const pageIndex = ref(0)
const pageSize = 10

const fetchUsers = async () => {
  const { data, error } = await apiCallWithLoading(() =>
    AdminUserController.listUsers({
      query: { pageIndex: pageIndex.value, pageSize },
    })
  )
  if (!error) {
    userPage.value = data
  }
}

const goToPage = (page: number) => {
  pageIndex.value = page
  fetchUsers()
}

const formatTime = (time: string | undefined): string => {
  if (!time) return "-"
  return new Date(time).toLocaleString()
}

onMounted(() => {
  fetchUsers()
})
</script>
