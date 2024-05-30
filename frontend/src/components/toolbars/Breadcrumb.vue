<template>
  <BasicBreadcrumb v-bind="{ ancestors: notePosition.ancestors }">
    <template #topLink>
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
import { PropType } from "vue";
import { NotePositionViewedByUser } from "@/generated/backend";
import BasicBreadcrumb from "../commons/BasicBreadcrumb.vue";

defineProps({
  notePosition: {
    type: Object as PropType<NotePositionViewedByUser>,
    required: true,
  },
});
</script>
