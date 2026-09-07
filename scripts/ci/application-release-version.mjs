const applicationVersion = /^v(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/
const tagRefPrefix = 'refs/tags/'

export function isApplicationTag(tag) {
  return typeof tag === 'string' && applicationVersion.test(tag)
}

export function applicationTagFromRef(ref) {
  if (typeof ref !== 'string' || !ref.startsWith(tagRefPrefix)) return
  const tag = ref.slice(tagRefPrefix.length)
  if (isApplicationTag(tag)) return tag
}

export function compareApplicationVersionsDescending(left, right) {
  const leftParts = left.slice(1).split('.').map(BigInt)
  const rightParts = right.slice(1).split('.').map(BigInt)
  for (let index = 0; index < leftParts.length; index++) {
    if (leftParts[index] > rightParts[index]) return -1
    if (leftParts[index] < rightParts[index]) return 1
  }
  return 0
}
