<template>
  <Breadcrumb v-bind="{ noteTopology, ancestorFolders }">
    <template #topLink>
      <li v-if="fromBazaar">
        <router-link :to="{ name: 'bazaar' }">Bazaar</router-link>
      </li>
      <template v-else>
        <template v-if="circle">
          <li>
            <router-link
              :to="{
                name: 'circleShow',
                params: { circleId: circle.id },
              }"
              >{{ circle.name }}</router-link
            >
          </li>
        </template>
      </template>
    </template>
  </Breadcrumb>
</template>

<script setup lang="ts">
import type {
  Circle,
  FolderTrailSegment,
  NoteTopology,
} from "@generated/doughnut-backend-api"
import type { PropType } from "vue"

defineProps({
  noteTopology: {
    type: Object as PropType<NoteTopology>,
    required: true,
  },
  ancestorFolders: {
    type: Array as PropType<FolderTrailSegment[]>,
    default: () => [],
  },
  circle: {
    type: Object as PropType<Circle>,
    required: false,
  },
  fromBazaar: {
    type: Boolean,
    required: false,
  },
})
</script>
