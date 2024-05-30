<template>
  <BasicBreadcrumb v-bind="{ ancestors }">
    <template #topLink>
      <li v-if="fromBazaar" class="breadcrumb-item">
        <router-link :to="{ name: 'bazaar' }">Bazaar</router-link>
      </li>
      <template v-else>
        <li class="breadcrumb-item" v-if="!circle">
          <router-link :to="{ name: 'notebooks' }">My Notes</router-link>
        </li>
        <template v-else>
          <li class="breadcrumb-item">
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
  </BasicBreadcrumb>
</template>

<script setup lang="ts">
import { PropType, computed } from "vue";
import { NoteTopic, Circle } from "@/generated/backend";
import BasicBreadcrumb from "../commons/BasicBreadcrumb.vue";

const props = defineProps({
  noteTopic: {
    type: Object as PropType<NoteTopic>,
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
