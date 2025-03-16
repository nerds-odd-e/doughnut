<template>
  <h3>Move notebook to</h3>
  <div class="daisy:overflow-auto daisy:max-h-40" style="max-height: 200px">
    <ul class="daisy:menu">
      <li
        class="daisy-menu-item"
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
import type { PropType } from "vue"
import { ref, onMounted } from "vue"
import type { Circle, Notebook } from "@/generated/backend"
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
