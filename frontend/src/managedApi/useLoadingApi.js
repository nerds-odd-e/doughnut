import { ref } from "vue";
import api from "./api";

export default function (initalLoading=false) {
  const loading = ref(initalLoading)
  return {
    apiExp() { return api(this) },
    loading,
  };
}