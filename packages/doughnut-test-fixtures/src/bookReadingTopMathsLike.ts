import type {
  BookBlockFull,
  PdfLocatorFull,
} from '@generated/doughnut-backend-api'
import { pdfLocatorPageIndexOnly, pdfLocatorWithBbox } from './pdfLocatorFull'

export const TOP_MATHS_LIKE_BLOCK_IDS = [101, 102, 103, 104, 105, 106] as const

const preorderFirstLocators: PdfLocatorFull[] = [
  pdfLocatorPageIndexOnly(0),
  pdfLocatorWithBbox(0, [48, 72, 564, 200]),
  pdfLocatorWithBbox(0, [48, 520, 564, 756]),
  pdfLocatorPageIndexOnly(1),
  pdfLocatorPageIndexOnly(1),
  pdfLocatorPageIndexOnly(1),
]

export function topMathsLikePreorderFirstLocatorAt(
  index: number
): PdfLocatorFull {
  return preorderFirstLocators[index]!
}

export function topMathsLikeBlockRows(options: {
  depth?: number
  contentLocatorsForIndex: (index: number, blockId: number) => PdfLocatorFull[]
}): BookBlockFull[] {
  const depth = options.depth ?? 0
  return TOP_MATHS_LIKE_BLOCK_IDS.map((id, i) => ({
    id,
    depth,
    title: `Section ${i + 1}`,
    contentLocators: options.contentLocatorsForIndex(i, id),
    contentBlocks: [],
  }))
}

export function topMathsLikeFlatBlocks(options?: {
  firstBlockHasNoDirectContent?: boolean
  lastBlockHasDirectContent?: boolean
}): BookBlockFull[] {
  const lastIdx = TOP_MATHS_LIKE_BLOCK_IDS.length - 1
  return topMathsLikeBlockRows({
    contentLocatorsForIndex: (i) => {
      if (i === 0) {
        return options?.firstBlockHasNoDirectContent
          ? [pdfLocatorWithBbox(0, [0, 0, 0, 0])]
          : [pdfLocatorPageIndexOnly(0)]
      }
      if (i === lastIdx && options?.lastBlockHasDirectContent) {
        return [
          topMathsLikePreorderFirstLocatorAt(i),
          pdfLocatorWithBbox(1, [48, 200, 564, 500]),
        ]
      }
      return [topMathsLikePreorderFirstLocatorAt(i)]
    },
  })
}
