<template>
  <NoteBreadcrumb :ancestors="ancestors">
    <template v-slot:topLink>
      <template v-if="!!circle">
        <li class="breadcrumb-item">
          <router-link :to="{ name: 'circles' }">Circles</router-link>
        </li>
        <li class="breadcrumb-item">
          <router-link
            :to="{ name: 'circleShow', params: { circleId: circle.id } }"
            >{{ circle.name }}</router-link
          >
        </li>
      </template>
      <li class="breadcrumb-item" v-else>
        <router-link :to="{ name: 'notebooks' }">Top</router-link>
      </li>
    </template>
    <template v-slot:additional>
      <slot />
    </template>
  </NoteBreadcrumb>
</template>

<script setup>
import { computed } from "@vue/reactivity";
import NoteBreadcrumb from "./NoteBreadcrumb.vue";
const props = defineProps({ ancestors: Array, notebook: Object });
const circle = computed(() =>
  !!props.notebook ? props.notebook.ownership.circle : null
);
</script>
