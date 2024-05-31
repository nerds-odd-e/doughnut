<template>
  <Breadcrumb v-bind="{ noteTopic }">
    <template #topLink>
      <li v-if="fromBazaar" class="breadcrumb-item">
        <router-link :to="{ name: 'bazaar' }">Bazaar</router-link>
      </li>
      <template v-else>
        <li class="breadcrumb-item" v-if="!circle">
          <router-link
            v-if="fromBazaar !== undefined"
            :to="{ name: 'notebooks' }"
            >My Notes</router-link
          >
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
  </Breadcrumb>
</template>

<script setup lang="ts">
import { PropType } from "vue";
import { NoteTopic, Circle } from "@/generated/backend";
import Breadcrumb from "./Breadcrumb.vue";

defineProps({
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
</script>
