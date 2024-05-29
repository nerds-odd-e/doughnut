<template>
  <template v-if="note.targetForTopic">
    <span style="font-size: 50%">
      {{ linkType }}
    </span>
    <SvgLinkTypeIcon :link-type="linkType" :inverse-icon="true" />
    &nbsp;
    <span>
      {{ note.targetForTopic.topicConstructor }}
    </span>
  </template>
  <template v-else>
    {{ note.topic }}
  </template>
</template>

<script setup lang="ts">
import { Note } from "@/generated/backend";
import { PropType, computed, ref } from "vue";
import SvgLinkTypeIcon from "@/components/svgs/SvgLinkTypeIcon.vue";

const props = defineProps({
  note: { type: Object as PropType<Note>, required: true },
});

const reactiveProps = ref(props);

const linkType = computed(() =>
  reactiveProps.value.note.topicConstructor.substring(1),
);
</script>
