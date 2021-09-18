<template>
  <NoteOwnerBreadcrumb
    v-if="owns"
    :ancestors="ancestors"
    :notebook="notebook"
  />
  <NoteBreadcrumb v-else :ancestors="ancestors">
    <template #topLink>
      <li class="breadcrumb-item">
        <router-link :to="{ name: 'bazaar' }">Bazaar</router-link>
      </li>
    </template>
  </NoteBreadcrumb>
</template>

<script>
import NoteBreadcrumb from "./NoteBreadcrumb.vue";
import NoteOwnerBreadcrumb from "./NoteOwnerBreadcrumb.vue";

export default {
  name: "Breadcrumb",
  props: {
    ancestors: Array,
    notebook: Object,
    noteTitle: String,
    owns: { type: Boolean, required: true },
  },
  components: {
    NoteBreadcrumb,
    NoteOwnerBreadcrumb,
  },
};
</script>
