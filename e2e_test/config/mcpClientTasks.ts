import { join } from 'node:path'
import mcpClient from '../support/mcp_client'
import { CLI_E2E_PNPM_SPAWN_ENV, runShellCommandSync } from './cliE2eRepo'

export function mcpClientTasks(repoRoot: string) {
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
      return await mcpClient.disconnectMcp()
    },
    async bundleMcpServer() {
      const mcpServerDir = join(repoRoot, 'mcp-server')
      try {
        runShellCommandSync('pnpm bundle', {
          cwd: mcpServerDir,
          env: {
            ...process.env,
            ...CLI_E2E_PNPM_SPAWN_ENV,
          },
        })
        return true
      } catch (error) {
        console.error('Failed to bundle MCP server:', error)
        throw error
      }
    },
  }
}
