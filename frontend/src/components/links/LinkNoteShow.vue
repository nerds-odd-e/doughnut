<template>
  <BreadcrumbAsync v-bind="{ noteId: noteTopic.id }" />
  <h5>
    <NoteTopicWithLink v-bind="{ noteTopic }" class="card-title" />
  </h5>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import { NoteTopic } from "@/generated/backend";
import BreadcrumbAsync from "../toolbars/BreadcrumbAsync.vue";

export default defineComponent({
  name: "LinkNoteShow",
  props: {
    noteTopic: { type: Object as PropType<NoteTopic>, required: true },
  },
  components: { BreadcrumbAsync },
});
</script>
