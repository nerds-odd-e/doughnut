import type {
  BookBlockFull,
  PageBboxFull,
} from '@generated/doughnut-backend-api'
import {
  pageBboxPageIndexOnly,
  pageBboxWithNormalizedBbox,
} from './pageBboxFull'

export const TOP_MATHS_LIKE_BLOCK_IDS = [101, 102, 103, 104, 105, 106] as const

const preorderFirstBboxes: PageBboxFull[] = [
  pageBboxPageIndexOnly(0),
  pageBboxWithNormalizedBbox(0, [48, 72, 564, 200]),
  pageBboxWithNormalizedBbox(0, [48, 520, 564, 756]),
  pageBboxPageIndexOnly(1),
  pageBboxPageIndexOnly(1),
  pageBboxPageIndexOnly(1),
]

export function topMathsLikePreorderFirstBboxAt(index: number): PageBboxFull {
  return preorderFirstBboxes[index]!
}

export function topMathsLikeBlockRows(options: {
  depth?: number
  allBboxesForIndex: (index: number, blockId: number) => PageBboxFull[]
}): BookBlockFull[] {
  const depth = options.depth ?? 0
  return TOP_MATHS_LIKE_BLOCK_IDS.map((id, i) => ({
    id,
    depth,
    title: `Section ${i + 1}`,
    allBboxes: options.allBboxesForIndex(i, id),
  }))
}

export function topMathsLikeFlatBlocks(options?: {
  firstBlockHasNoDirectContent?: boolean
}): BookBlockFull[] {
  return topMathsLikeBlockRows({
    allBboxesForIndex: (i) => {
      if (i === 0) {
        return options?.firstBlockHasNoDirectContent
          ? [pageBboxWithNormalizedBbox(0, [0, 0, 0, 0])]
          : [pageBboxPageIndexOnly(0)]
      }
      return [topMathsLikePreorderFirstBboxAt(i)]
    },
  })
}
