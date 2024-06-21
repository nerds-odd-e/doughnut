<template>
  <template v-if="noteTopic.targetNoteTopic">
    <span class="link-type" style="font-size: 50%">
      {{ linkType }}
    </span>
    <SvgLinkTypeIcon :link-type="linkType" :inverse-icon="true" />
    &nbsp;
    <span>
      <NoteTopicComponent
        v-if="iconizedTarget"
        v-bind="{ noteTopic: noteTopic.targetNoteTopic }"
      />
      <NoteTopicWithLink
        class="hover-underline"
        v-bind="{
          noteTopic: noteTopic.targetNoteTopic,
          iconized: iconizedTarget,
        }"
      />
    </span>
  </template>
  <template v-else>
    <span class="topic-text">{{ topic }} </span>
  </template>
</template>

<script setup lang="ts">
import { NoteTopic } from "@/generated/backend"
import { PropType, computed, ref } from "vue"

const props = defineProps({
  noteTopic: { type: Object as PropType<NoteTopic>, required: true },
})

const reactiveProps = ref(props)

const linkType = computed(() =>
  reactiveProps.value.noteTopic.topicConstructor.substring(1),
)
const topic = computed(() =>
  reactiveProps.value.noteTopic.topicConstructor?.replace(
    "%P",
    `[${reactiveProps.value.noteTopic.parentNoteTopic?.topicConstructor}]`,
  ),
)
const iconizedTarget = computed(
  () => !!reactiveProps.value.noteTopic.shortDetails,
)
</script>

<style scoped>
.hover-underline:hover {
  text-decoration: underline !important;
}
</style>
