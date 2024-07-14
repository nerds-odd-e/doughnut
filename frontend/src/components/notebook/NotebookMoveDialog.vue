<template>
  <h2>Move notebook to</h2>
  <h3>Move notebook to</h3>
  <div class="overflow-auto" style="max-height: 200px">
    <ul class="list-group">
      <li class="list-group-item" v-for="circle in circles" :key="circle.id">
        {{ circle.name }}
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import { Circle } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"

const { managedApi } = useLoadingApi()

const circles = ref<Circle[]>([])

onMounted(async () => {
  circles.value = await managedApi.restCircleController.index()
})
</script>
