# Rule: Always Use context-mode MCP Tools & Install Release Builds

To protect the context window from flooding and optimize session resource usage, the agent must strictly follow the context-mode routing rules:

1. **Do NOT use `curl` or `wget`** via `run_command`. Instead, use `ctx_fetch_and_index` or run HTTP calls in the sandbox using `ctx_execute`.
2. **Do NOT run inline HTTP calls** (e.g. `node -e "fetch"`, `python -c "requests.get"`) via `run_command`. Run them in the sandbox instead.
3. **Do NOT use `read_url_content`** for fetching large web pages. Fetch and index them using `ctx_fetch_and_index` and query using `ctx_search`.
4. **Use sandbox execution** for commands yielding large output (>20 lines) or searching files (e.g. `grep`):
   - Use `ctx_batch_execute` or `ctx_execute` (with language `shell`) instead of direct `run_command` when output is large.
   - Use `ctx_execute_file` for analyzing, exploring, or summarizing file contents.
5. **Use FTS5 Search**: Use `ctx_search` to query indexed content in one batch rather than making many individual file reads.

## FILE SYSTEM Rules — MANDATORY

- **Only use filesystem MCP**: All file system operations (listing, reading, writing, editing) must be routed through the `filesystem` MCP tool server (e.g. `mcp_filesystem_read_file`, `mcp_filesystem_write_file`, `mcp_filesystem_edit_file`, `mcp_filesystem_list_directory`) instead of standard file read/write tools where possible.

## BUILD Rules — MANDATORY

- **Always install Release builds, never Debug builds**: When asked to build/install the APK, always compile the release build using `./gradlew assembleRelease` and install it.
- **Update Release builds**: Use `adb install -r` to update the release build on the connected device. If there's a signature mismatch (e.g. from an existing debug build), uninstall the old package first, then install the release build.
