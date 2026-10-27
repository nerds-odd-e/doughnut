import mcpClient from '../support/mcp_client'

export function mcpClientTasks() {
  return {
    mcpClientConnectionInfo() {
      return mcpClient.connectionInfo()
    },
    async connectMcpHttps({
      baseUrl,
      accessToken,
    }: {
      baseUrl: string
      accessToken: string
    }) {
      return await mcpClient.connectMcpHttps({
        baseUrl,
        accessToken,
      })
    },
    async callMcpToolWithParams({
      apiName,
      params,
    }: {
      apiName: string
      params: Record<string, unknown>
    }) {
      return await mcpClient.callMcpToolWithParams({ apiName, params })
    },
    async disconnectMcp() {
      await mcpClient.disconnectMcp()
      return null
    },
  }
}
