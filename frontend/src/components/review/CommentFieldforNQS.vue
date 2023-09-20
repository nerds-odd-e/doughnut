<template>
  <h3>Add comment</h3>
  <div>
    <span>
      Sending this question for fine tuning the question generation model will
      make this note and question visible to admin. Are you sure?
    </span>
    <textarea id="CommentField" placeholder="Add some comment" />
    <button type="submit" value="Confirm" class="btn btn-primary">
      Submit
    </button>
  </div>
</template>

<script>
export default defineComponent({
  props: {
    id: String,
    type: String,
    value: String,
  },
});
</script>
