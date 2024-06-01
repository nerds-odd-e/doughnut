<template>
  <template v-if="noteTopic.targetNoteTopic">
    <span style="font-size: 50%">
      {{ linkType }}
    </span>
    <SvgLinkTypeIcon :link-type="linkType" :inverse-icon="true" />
    &nbsp;
    <span>
      <NoteTopicWithLink v-bind="{ noteTopic: noteTopic.targetNoteTopic }" />
    </span>
  </template>
  <template v-else>
    {{ topic }}
  </template>
</template>

<script setup lang="ts">
import { NoteTopic } from "@/generated/backend";
import { PropType, computed, ref } from "vue";
import SvgLinkTypeIcon from "@/components/svgs/SvgLinkTypeIcon.vue";
import NoteTopicWithLink from "../NoteTopicWithLink.vue";

const props = defineProps({
  noteTopic: { type: Object as PropType<NoteTopic>, required: true },
});

const reactiveProps = ref(props);

const linkType = computed(() =>
  reactiveProps.value.noteTopic.topicConstructor.substring(1),
);
const topic = computed(() =>
  reactiveProps.value.noteTopic.topicConstructor?.replace(
    "%P",
    `[${reactiveProps.value.noteTopic.parentNoteTopic?.topicConstructor}]`,
  ),
);
</script>
