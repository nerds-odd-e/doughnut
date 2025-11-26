import { reactive } from "vue"

/**
 * Domain model for search state management
 */
export class SearchState {
  private state = reactive({
    isSearchInProgress: false,
  })

  get isSearchInProgress(): boolean {
    return this.state.isSearchInProgress
  }

  startSearch(): void {
    this.state.isSearchInProgress = true
  }

  completeSearch(): void {
    this.state.isSearchInProgress = false
  }
}
