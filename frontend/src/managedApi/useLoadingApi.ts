import { ref } from "vue";
import api from "./api";

export default function (options={initalLoading: false, hasFormError: false}) {
  const loading = ref(options.initalLoading)
  const formError = options.hasFormError ? ref({}) : null
  return {
    apiExp() { return api(this) },
    loading,
    formError,
  };
}