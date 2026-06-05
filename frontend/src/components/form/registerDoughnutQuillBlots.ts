import Quill from "quill"

interface EmbedBlotInstance {
  // Blot instance interface
}
type EmbedBlotConstructor = new (
  node: Node,
  value?: unknown
) => EmbedBlotInstance

interface DeltaInstance {
  insert(content: unknown): DeltaInstance
}
type DeltaConstructor = new (ops?: unknown) => DeltaInstance

const Embed = Quill.import("blots/embed") as unknown as EmbedBlotConstructor
const BlockEmbed = Quill.import(
  "blots/block/embed"
) as unknown as EmbedBlotConstructor
const Delta = Quill.import("delta") as unknown as DeltaConstructor

class SoftLineBreakBlot extends Embed {
  static blotName = "softbreak"
  static tagName = "br"
  static className = "softbreak"
}

class HorizontalRuleBlot extends Embed {
  static blotName = "horizontalrule"
  static tagName = "hr"
}

class TableBlot extends BlockEmbed {
  static blotName = "table"
  static tagName = "table"

  static create(value: string | { html: string }) {
    // @ts-expect-error - Quill's BlockEmbed class has static create method but types don't reflect it
    const node = super.create() as HTMLElement
    const html = typeof value === "string" ? value : value.html
    node.innerHTML = html
    node.setAttribute("contenteditable", "false")
    return node
  }

  static value(node: HTMLElement) {
    return { html: node.innerHTML }
  }
}

/** Preserves `<mark>` cloze masks from recall stems when loading HTML into Quill. */
const Inline = Quill.import("blots/inline") as typeof SoftLineBreakBlot
class MarkBlot extends Inline {
  static blotName = "mark"
  static tagName = "mark"
}

let registered = false

/** Registers Doughnut-specific Quill blots once per page load. */
export function registerDoughnutQuillBlots() {
  if (registered) return
  registered = true

  Quill.register(
    SoftLineBreakBlot as unknown as Parameters<typeof Quill.register>[0],
    true
  )
  Quill.register(
    HorizontalRuleBlot as unknown as Parameters<typeof Quill.register>[0],
    true
  )
  Quill.register(
    TableBlot as unknown as Parameters<typeof Quill.register>[0],
    true
  )
  Quill.register(
    MarkBlot as unknown as Parameters<typeof Quill.register>[0],
    true
  )
}

export const doughnutQuillBrMatcher = () =>
  new Delta().insert({ softbreak: true })
