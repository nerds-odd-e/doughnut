import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'
import { Client } from '@modelcontextprotocol/sdk/client/index.js'
import { SSEClientTransport } from '@modelcontextprotocol/sdk/client/sse.js'

// import { MCPClient } from './client'

interface InstructionResponse {
  content: Array<{
    text: string
  }>
  status: string
  // 必要に応じて他のプロパティも追加
}

let client: Client
let instructionResponse: InstructionResponse

Given('MCP server is running', () => {
  // MCPサーバーの状態を確認
  cy.task('checkMcpServerStatus').then((status) => {
    expect(status).to.eq('running')
  })
})

When('Call instruction API by MCP Client', () => {
  // MCPクライアントを初期化
  client = new Client(
    {
      name: 'example-client',
      version: '1.0.0',
    },
    {
      capabilities: {},
    }
  )

  const transport = new SSEClientTransport(new URL('http://localhost:9081/sse'))

  const asyncFunction = async () => {
    await client.connect(transport)
    const result = await client.callTool({
      name: 'getInstruction',
    })
    return result
  }

  cy.wrap(asyncFunction()).then((response) => {
    // responseから適切にデータを取り出す
    instructionResponse = response as InstructionResponse
    console.log('Response:', response)
  })
})

Then('Return Doughnut instruction', () => {
  // レスポンスを検証
  expect(instructionResponse.content).to.have.length.at.least(1)
  expect(instructionResponse.content[0]!.text).to.equal('"hello"')
})

Then('Response has valid structure', () => {
  // レスポンス全体の構造を検証
  expect(instructionResponse).to.be.an('object')
  expect(instructionResponse).to.have.property('content')
  expect(instructionResponse.content).to.be.an('array')
  expect(instructionResponse.content).to.have.length.at.least(1)
  expect(instructionResponse.status).to.equal('success')
})

Then('Response content contains expected text', () => {
  // コンテンツの詳細を検証
  expect(instructionResponse.content).to.have.length.at.least(1)
  const firstContent = instructionResponse.content[0]!
  expect(firstContent).to.have.property('text')
  expect(firstContent.text).to.be.a('string')
  expect(firstContent.text).to.equal('"hello"')
})
