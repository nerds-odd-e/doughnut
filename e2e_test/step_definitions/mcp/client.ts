import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { SSEClientTransport } from "@modelcontextprotocol/sdk/client/sse.js";

export class MCPClient {
  private client: Client;
  private transport: SSEClientTransport;

  constructor() {
    this.client = new Client({
      name: "example-client",
      version: "1.0.0"
    }, {
      capabilities: {}
    });
  
    this.transport = new SSEClientTransport(
      new URL("http://localhost:9081/sse")
    );
  }

  async getInstruction() {
    await this.client.connect(this.transport);
    const instruction = this.client.getInstructions();
    return instruction;
  }
}