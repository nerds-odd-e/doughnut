<template>
  <Breadcrumb v-bind="{ noteTopic }" />
  <h5>
    <NoteTopicWithLink v-bind="{ noteTopic }" class="card-title" />
  </h5>
</template>

<script lang="ts">
import { NoteTopic } from "@/generated/backend"
import { PropType, defineComponent } from "vue"
import Breadcrumb from "../toolbars/Breadcrumb.vue"

export default defineComponent({
  name: "LinkNoteShow",
  props: {
    noteTopic: { type: Object as PropType<NoteTopic>, required: true },
  },
  components: { Breadcrumb },
})
</script>
