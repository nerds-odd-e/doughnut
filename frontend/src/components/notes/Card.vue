<template>
  <div
    role="card"
    class="daisy-card bg-base-100 shadow-xl hover:shadow-2xl hover:bg-base-300 transition-all"
  >
    <slot name="cardHeader" />
      <div class="daisy-card-body p-4">
    <router-link :to="noteShowLocation" class="no-underline">
        <h5 class="daisy-card-title">
          <NoteTitleWithLink v-bind="{ noteTopology }" />
        </h5>
    </router-link>
    <div class="daisy-card-actions justify-end" v-if="$slots.button">
      <slot name="button" :note-title="noteTopology" />
    </div>
      </div>
  </div>
</template>

<script setup lang="ts">
import type { PropType } from "vue"
import { computed } from "vue"
import type { NoteTopology } from "@generated/doughnut-backend-api"
import { noteShowLocation as noteShowLocationForRoute } from "@/routes/noteShowLocation"
import NoteTitleWithLink from "./NoteTitleWithLink.vue"

const props = defineProps({
  noteTopology: {
    type: Object as PropType<NoteTopology>,
    required: true,
  },
})

const noteShowLocation = computed(() =>
  noteShowLocationForRoute(props.noteTopology.id)
)
</script>
