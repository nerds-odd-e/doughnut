import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor';
import { MCPClient } from './client';

let mcpClient: MCPClient;
let instructionResponse: any;

Given('MCP server is running', () => {
  // MCPサーバーの状態を確認
  cy.task('checkMcpServerStatus').then((status) => {
    expect(status).to.eq('running');
  });
});

When('Call instruction API by MCP Client', () => {
  // MCPクライアントを初期化
  mcpClient = new MCPClient();
  
  // 指示を取得
  cy.wrap(mcpClient.getInstruction()).then((response) => {
    instructionResponse = response;
  });
});

Then('Return Doughnut instruction', () => {
  // レスポンスを検証
  expect(instructionResponse).to.not.be.null;
  expect(instructionResponse).to.have.property('instruction');
  expect(instructionResponse.instruction).to.be.a('string');
  expect(instructionResponse.instruction).to.not.be.empty;
}); 