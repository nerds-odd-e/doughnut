export function unwrapData<T>(result: T | { data: T } | undefined): T {
  if (result && typeof result === 'object' && 'data' in result) {
    return (result as { data: T }).data
  }
  return result as T
}
