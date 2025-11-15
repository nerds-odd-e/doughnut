import type { DoughnutApi } from './DoughnutApiExport.js'

export type ToolResponseContent = { type: 'text'; text: string }

export type ToolResponse = {
  content: ToolResponseContent[]
}

export type ServerContext = {
  api: DoughnutApi
  authToken?: string
}

export type ToolDescriptor = {
  name: string
  description: string
  inputSchema: Record<string, unknown>
  handle: (
    ctx: ServerContext,
    args: Record<string, unknown>,
    request?: unknown
  ) => Promise<ToolResponse> | ToolResponse
}
