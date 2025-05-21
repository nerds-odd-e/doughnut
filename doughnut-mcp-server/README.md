# doughnut-mcp-server MCP Server

A Model Context Protocol server for doughnut

This is a TypeScript-based MCP server that implements a simple notes system. It demonstrates core MCP concepts by providing:

- Tools for get instruction, and proxying to external APIs.

## Features

### Tools
- `get_instruction` - Get instruction of Doughnut
  - Takes an empty object as parameter
  - Returns instruction for Doughnut

- Proxy: Any other tool call will be proxied to a configured API endpoint, with a `sessionId` parameter automatically added.

## How to Add a New API Tool

To add a new API/tool to the MCP server, edit `src/index.ts`:

1. **Define your tool in the ListTools handler**  
   Add your tool to the `tools` array in the `ListToolsRequestSchema` handler:
   ```ts
   server.setRequestHandler(ListToolsRequestSchema, async () => {
     return {
       tools: [
         {
           name: 'get_instruction',
           description: 'Get instruction',
           inputSchema: { type: 'object' },
         },
         {
           name: 'your_tool_name',
           description: 'Describe your tool',
           inputSchema: { type: 'object' }, // or your schema
         },
       ],
     }
   })
   ```

2. **Handle your tool in the CallTool handler**  
   Add a case for your tool in the `CallToolRequestSchema` handler:
   ```ts
   server.setRequestHandler(CallToolRequestSchema, async (request) => {
     switch (request.params.name) {
       case 'get_instruction':
         // existing code...
         break;
       case 'your_tool_name':
         // Implement your tool logic here
         return {
           content: [
             {
               type: 'text',
               text: 'Your tool response here',
             },
           ],
         }
       default:
         // Proxy logic (already implemented)
     }
   })
   ```
   - If you do not add a case, the request will be proxied to the configured API endpoint.

3. **Rebuild the bundle**  
   After making changes, rebuild the bundle:
   ```
   cd doughnut-mcp-server
   CURSOR_DEV=true nix develop -c pnpm exec esbuild src/index.ts --bundle --platform=node --outfile=build/mcp-server.bundle.js --format=esm
   ```

4. **Host the bundle**  
   Upload `build/mcp-server.bundle.js` to your public server or CDN.

## MCP Server Configuration for Remote Bundle

To use the MCP server bundle remotely (e.g., with Cursor), update your MCP server configuration as follows:

```json
{
  "mcpServers": {
    "doughnut": {
      "disabled": false,
      "timeout": 60,
      "command": "node",
      "args": [
        "-e",
        "import('node-fetch').then(f=>f.default('https://doughnut.odd-e.com/assets/js/mcp-server.bundle.js').then(r=>r.text()).then(eval))"
      ],
      "transportType": "stdio"
    }
  }
}
```

- This configuration will fetch and execute the MCP server bundle from the provided URL using Node.js and node-fetch.
- Make sure the URL points to your hosted `mcp-server.bundle.js`.

## Development

- To run locally, you can use:
  ```
  CURSOR_DEV=true nix develop -c node src/index.ts
  ```
- To build the bundle for remote use:
  ```
  CURSOR_DEV=true nix develop -c pnpm exec esbuild src/index.ts --bundle --platform=node --outfile=build/mcp-server.bundle.js --format=esm
  ```

## Installation

- Install dependencies:
  ```
  CURSOR_DEV=true nix develop -c pnpm install
