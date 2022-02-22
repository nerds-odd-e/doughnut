<template>
  <div class="card">
  <span>{{ content }}</span>
  </div>
</template>

<script>

export default {
  props: {
    content: { type: String, required: true }, 
  },
}
</script>