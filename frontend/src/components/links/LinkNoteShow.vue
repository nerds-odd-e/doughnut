<template>
  <Breadcrumb v-bind="{ noteTopology }" />
  <h5>
    <NoteTopicWithLink v-bind="{ noteTopology }" class="card-title" />
  </h5>
</template>

<script lang="ts">
import { NoteTopology } from "@/generated/backend"
import type { PropType } from "vue"
import { defineComponent } from "vue"
import Breadcrumb from "../toolbars/Breadcrumb.vue"

export default defineComponent({
  name: "LinkNoteShow",
  props: {
    noteTopology: { type: Object as PropType<NoteTopology>, required: true },
  },
  components: { Breadcrumb },
})
</script>
