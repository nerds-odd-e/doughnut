<template>
  <div>
    <form @submit.prevent="onSave()">
      <div class="cerified-by-input-container">
        <span>Cerified By</span>
        <input type="text" v-model="cerifiedBy" />
      </div>
      <button type="submit" class="btn btn-primary">Save</button>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue"

const cerifiedBy = ref("")

const onSave = () => {
  // TODO call api save cerificate_by
}
</script>

<style>
.cerified-by-input-container {
  display: flex;
  column-gap: 4px;
  align-items: center;
  margin-bottom: 8px;
}
</style>