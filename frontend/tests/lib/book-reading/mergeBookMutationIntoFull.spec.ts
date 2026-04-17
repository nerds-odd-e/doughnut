import { mergeBookMutationIntoFull } from "@/lib/book-reading/mergeBookMutationIntoFull"
import type { BookMutationResponse } from "@generated/doughnut-backend-api"
import makeMe from "doughnut-test-fixtures/makeMe"
import { describe, expect, it } from "vitest"

describe("mergeBookMutationIntoFull", () => {
  it("maps short-form epub and pdf locators to Full", () => {
    const block = makeMe.aBookBlock
      .id(1)
      .contentLocators([
        { type: "PdfLocator_Full", pageIndex: 0, bbox: [0, 0, 1, 1] },
      ])
      .do()
    const book = makeMe.aBook.blocks([block]).do()
    const mutation: BookMutationResponse = {
      ...book,
      blocks: [
        {
          id: 1,
          depth: 0,
          title: "T",
          contentLocators: [
            { type: "epub", href: "a.xhtml", fragment: "x" },
            {
              type: "pdf",
              pageIndex: 2,
              bbox: [1, 2, 3, 4],
              contentBlockId: 5,
            },
          ],
        },
      ],
    }
    const merged = mergeBookMutationIntoFull(book, mutation)
    expect(merged.blocks).toHaveLength(1)
    expect(merged.blocks[0]!.contentLocators).toEqual([
      { type: "EpubLocator_Full", href: "a.xhtml", fragment: "x" },
      {
        type: "PdfLocator_Full",
        pageIndex: 2,
        bbox: [1, 2, 3, 4],
        contentBlockId: 5,
        derivedTitle: undefined,
      },
    ])
  })

  it("passes through locators already tagged EpubLocator_Full or PdfLocator_Full", () => {
    const epubFull = {
      type: "EpubLocator_Full" as const,
      href: "ch.xhtml",
      fragment: "sec",
    }
    const pdfFull = {
      type: "PdfLocator_Full" as const,
      pageIndex: 3,
      bbox: [0, 0, 100, 100],
      contentBlockId: 9,
      derivedTitle: "D",
    }
    const block = makeMe.aBookBlock.id(1).contentLocators([epubFull]).do()
    const book = makeMe.aBook.blocks([block]).do()
    const mutation = {
      ...book,
      blocks: [
        {
          id: 1,
          depth: 0,
          title: "T",
          contentLocators: [epubFull, pdfFull],
        },
      ],
    } as unknown as BookMutationResponse
    const merged = mergeBookMutationIntoFull(book, mutation)
    expect(merged.blocks).toHaveLength(1)
    const locators = merged.blocks[0]!.contentLocators
    expect(locators).toEqual([epubFull, pdfFull])
    expect(locators[0]).toBe(epubFull)
    expect(locators[1]).toBe(pdfFull)
  })

  it("keeps previous contentLocators when the mutation row omits them", () => {
    const prevLocators = [
      { type: "EpubLocator_Full" as const, href: "x.xhtml" },
    ]
    const block = makeMe.aBookBlock.id(1).contentLocators(prevLocators).do()
    const book = makeMe.aBook.blocks([block]).do()
    const mutation: BookMutationResponse = {
      ...book,
      blocks: [{ id: 1, depth: 0, title: "New" }],
    }
    const merged = mergeBookMutationIntoFull(book, mutation)
    expect(merged.blocks).toHaveLength(1)
    expect(merged.blocks[0]!.contentLocators).toEqual(prevLocators)
  })
})
