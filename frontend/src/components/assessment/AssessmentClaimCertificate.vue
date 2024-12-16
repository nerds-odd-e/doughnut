<template>
  <PopButton
    btn-class="daisy-btn daisy-btn-light"
    title="View Certificate"
  >
    <CertificatePopup
      :notebook-id="notebookId"
    >
    </CertificatePopup>
  </PopButton>
</template>

<script setup lang="ts">
defineProps({
  notebookId: {
    type: Number,
    required: true,
  },
})
</script>
