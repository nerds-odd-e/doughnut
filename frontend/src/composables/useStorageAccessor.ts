import { ref } from "vue"
import createNoteStorage, {
  type StorageAccessor,
} from "@/store/createNoteStorage"

const storageAccessor = ref<StorageAccessor>(createNoteStorage())

export function useStorageAccessor() {
  return storageAccessor
}
