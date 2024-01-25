<template>
  <router-link
    :to="{ name: 'noteShow', params: { noteId: note.id } }"
    class="text-decoration-none"
  >
    <NoteTopic v-bind="{ note, parentNote }" />
  </router-link>
</template>

<script lang="ts">
import { PropType, defineComponent } from "vue";
import NoteTopic from "./NoteTopic.vue";

export default defineComponent({
  props: {
    note: { type: Object as PropType<Generated.Note>, required: true },
    parentNote: { type: Object as PropType<Generated.Note> },
  },
  components: { NoteTopic },
});
</script>
