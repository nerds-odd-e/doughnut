<template>
  <div>
    <Cards :notes="notes">
      <template #cardHeader="{ note }">
        <span class="card-header d-flex flex-row-reverse p-0">
          <slot name="headerButtons" :note="note" />
        </span>
      </template>
      <template #button="{ note }">
        <slot name="button" :note="note" />
      </template>
    </Cards>
  </div>
</template>

<script>
import Cards from "./Cards.vue";

export default {
  name: "NoteCardWithHeaderAndButton",
  props: { notes: Array },
  components: { Cards },
};
</script>
