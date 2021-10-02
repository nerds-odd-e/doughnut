<template>
  <div>
    <div v-if="!!noteViewedByUser">
      <NoteViewedByUserWithoutChildren
        v-bind="{...note, breadcrumb}"
      />
    </div>

    <div v-if="!!linkViewedByUser">
      <div class="jumbotron py-4 mb-2">
        <LinkShow v-bind="linkViewedByUser">
          <LinkNob
            v-bind="{ link: linkViewedByUser }"
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
import { computed } from "@vue/reactivity";
const props = defineProps({
  noteViewedByUser: Object,
  linkViewedByUser: Object,
});

const breadcrumb = computed(()=>{
  const {noteBreadcrumbViewedByUser} = props.noteViewedByUser
  return noteBreadcrumbViewedByUser
})

const note = computed(()=>{
  const {noteItself} = props.noteViewedByUser
  return noteItself
})

</script>
