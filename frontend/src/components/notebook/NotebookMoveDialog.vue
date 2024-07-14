<template>
  <h2>Move notebook to</h2>
  <h3>Move notebook to</h3>
  <div class="overflow-auto" style="max-height: 200px">
    <ul class="list-group">
      <li
        class="list-group-item"
        v-for="circle in circles"
        :key="circle.id"
        @click="move(circle)"
      >
        {{ circle.name }}
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, PropType } from "vue"
import { Circle, Notebook } from "@/generated/backend"
import useLoadingApi from "@/managedApi/useLoadingApi"
import usePopups from "@/components/commons/Popups/usePopups"

const { managedApi } = useLoadingApi()
const { popups } = usePopups()

const props = defineProps({
  notebook: {
    type: Object as PropType<Notebook>,
    required: true,
  },
})

const circles = ref<Circle[]>([])

onMounted(async () => {
  circles.value = await managedApi.restCircleController.index()
})

const move = async (circle: Circle) => {
  if (await popups.confirm(`Move notebook to ${circle.name}?`)) {
    await managedApi.restNotebookController.moveToCircle(
      props.notebook.id,
      circle.id
    )
  }
}
</script>
