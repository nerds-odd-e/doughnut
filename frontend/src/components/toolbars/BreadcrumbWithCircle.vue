<template>
  <Breadcrumb v-bind="{ noteTopology }">
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
import type { Circle, NoteTopology } from "@/generated/backend"
import type { PropType } from "vue"

defineProps({
  noteTopology: {
    type: Object as PropType<NoteTopology>,
    required: true,
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
