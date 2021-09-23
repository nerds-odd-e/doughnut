<template>
  <div>
    <div v-if="!!noteViewedByUser">
      <NoteViewedByUserWithoutChildren
        v-bind="noteViewedByUser"
        @updated="$emit('updated')"
      />
    </div>

    <div v-if="!!linkViewedByUser">
      <div class="jumbotron py-4 mb-2">
        <LinkShow v-bind="linkViewedByUser" @updated="$emit('updated')">
          <LinkNob
            v-bind="{ link: linkViewedByUser }"
            @updated="$emit('updated')"
          />
          <span class="badge bg-light text-dark">
            {{ linkViewedByUser.linkTypeLabel }}</span
          >
        </LinkShow>
      </div>
    </div>
  </div>
</template>

<script setup>
import NoteViewedByUserWithoutChildren from "../notes/NoteViewedByUserWithoutChildren.vue";
import LinkShow from "../links/LinkShow.vue";
import LinkNob from "../links/LinkNob.vue";
const props = defineProps({
  noteViewedByUser: Object,
  linkViewedByUser: Object,
});
const emits = defineEmits(["updated"]);
</script>
