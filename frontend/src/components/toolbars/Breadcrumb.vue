<template>
  <BasicBreadcrumb v-bind="{ ancestors }">
    <template v-if="notePosition" #topLink>
      <li v-if="notePosition.fromBazaar" class="breadcrumb-item">
        <router-link :to="{ name: 'bazaar' }">Bazaar</router-link>
      </li>
      <template v-else>
        <li class="breadcrumb-item" v-if="!notePosition.circle">
          <router-link :to="{ name: 'notebooks' }">My Notes</router-link>
        </li>
        <template v-else>
          <li class="breadcrumb-item">
            <router-link
              :to="{
                name: 'circleShow',
                params: { circleId: notePosition.circle.id },
              }"
              >{{ notePosition.circle.name }}</router-link
            >
          </li>
        </template>
      </template>
    </template>
  </BasicBreadcrumb>
</template>

<script setup lang="ts">
import { PropType, computed } from "vue";
import { NotePositionViewedByUser, NoteTopic } from "@/generated/backend";
import BasicBreadcrumb from "../commons/BasicBreadcrumb.vue";

const props = defineProps({
  noteTopic: {
    type: Object as PropType<NoteTopic>,
    required: true,
  },
  notePosition: {
    type: Object as PropType<NotePositionViewedByUser>,
    required: false,
  },
});

const ancestors = computed(() => {
  const result: NoteTopic[] = [];
  let currentColor = props.noteTopic;
  while (currentColor.parentNoteTopic) {
    result.push(currentColor.parentNoteTopic);
    currentColor = currentColor.parentNoteTopic;
  }
  return result;
});
</script>
