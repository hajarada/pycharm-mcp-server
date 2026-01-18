# PyCharm Refactoring Bridge Plugin

A JetBrains plugin that exposes PyCharm's refactoring capabilities via an HTTP API.

## Features

This plugin starts an embedded HTTP server that provides endpoints for:

- **Rename** - Rename symbols across the project
- **Move** - Move classes/functions between modules
- **Extract Method** - Extract code into new methods
- **Extract Variable** - Extract expressions into variables
- **Inline** - Inline variables or methods
- **Change Signature** - Modify function signatures
- **Safe Delete** - Delete elements with usage checking
- **Find Usages** - Find all symbol usages

## Installation

### From JetBrains Marketplace

*(Coming soon)*

### Build from Source

```bash
./gradlew buildPlugin
```

The plugin ZIP will be in `build/distributions/`. Install via:
**Settings → Plugins → ⚙️ → Install Plugin from Disk...**

## Configuration

**Settings → Tools → Refactoring Bridge**

| Setting | Default | Description |
|---------|---------|-------------|
| Enabled | true | Start HTTP server on IDE launch |
| Port | 9876 | HTTP server port |
| Auth Token | (empty) | Optional bearer token for authentication |

## HTTP API

### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Health check |
| GET | `/projects` | List open projects |
| POST | `/refactor/rename` | Rename symbol |
| POST | `/refactor/move` | Move element |
| POST | `/refactor/extract-method` | Extract method |
| POST | `/refactor/extract-variable` | Extract variable |
| POST | `/refactor/inline` | Inline element |
| POST | `/refactor/change-signature` | Change signature |
| POST | `/refactor/safe-delete` | Safe delete |
| POST | `/find/usages` | Find usages |

### Example: Rename

**Request:**
```json
POST /refactor/rename
{
  "project": "/path/to/project",
  "file": "src/mymodule/service.py",
  "line": 42,
  "column": 8,
  "newName": "new_function_name",
  "searchInComments": true,
  "searchInStrings": false
}
```

**Response:**
```json
{
  "success": true,
  "changes": [
    {"file": "src/mymodule/service.py", "line": 42, "oldText": "old_name", "newText": "new_function_name"},
    {"file": "src/mymodule/test_service.py", "line": 15, "oldText": "old_name", "newText": "new_function_name"}
  ],
  "filesModified": 2,
  "usagesUpdated": 2
}
```

## Security

- Server binds to `127.0.0.1` only (localhost)
- Optional bearer token authentication
- All refactoring requests auto-save files first

## Development

### Prerequisites

- JDK 17+
- Gradle 8.5+

### Build

```bash
./gradlew build
```

### Run in Sandbox IDE

```bash
./gradlew runIde
```

### Run Tests

```bash
./gradlew test
```

## Architecture

```
src/main/kotlin/com/github/pycharm/refactoring/
├── RefactoringBridgePlugin.kt    # Plugin entry point
├── settings/                      # Settings UI and persistence
├── server/
│   ├── HttpServer.kt              # Embedded Ktor/Netty server
│   ├── RefactoringController.kt   # Request handlers
│   └── models/                    # Request/response DTOs
├── refactoring/
│   ├── RenameService.kt           # Rename operations
│   ├── MoveService.kt             # Move operations
│   ├── ExtractService.kt          # Extract method/variable
│   ├── InlineService.kt           # Inline operations
│   ├── SignatureService.kt        # Change signature
│   ├── SafeDeleteService.kt       # Safe delete
│   └── FindUsagesService.kt       # Find usages
└── util/
    ├── PsiUtils.kt                # PSI tree helpers
    └── ProjectUtils.kt            # Project context helpers
```

## License

MIT License
