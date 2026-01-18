"""Tool for finding usages in PyCharm."""

from pycharm_mcp.client import PyCharmBridgeError, PyCharmClient


async def find_usages(
    project_path: str,
    file_path: str,
    line: int,
    column: int,
) -> str:
    """
    Find all usages of a symbol across the project.

    This is useful for previewing what a refactoring would affect, or for
    understanding how a symbol is used throughout the codebase.

    Args:
        project_path: Absolute path to the project
        file_path: Path to the file containing the symbol
        line: Line number where the symbol is located (1-indexed)
        column: Column number (1-indexed)

    Returns:
        List of all usages with file locations and context.
    """
    client = PyCharmClient()
    try:
        response = await client.find_usages(
            project=project_path,
            file=file_path,
            line=line,
            column=column,
        )

        lines = [f"Usages of '{response.symbol}': {response.total_count} found", ""]

        # Group by file
        usages_by_file: dict[str, list[tuple[int, int, str, bool]]] = {}
        for usage in response.usages:
            if usage.file not in usages_by_file:
                usages_by_file[usage.file] = []
            usages_by_file[usage.file].append(
                (usage.line, usage.column, usage.text, usage.is_write_access)
            )

        for file_path, usages in usages_by_file.items():
            lines.append(f"📄 {file_path}:")
            for line_num, col, text, is_write in sorted(usages, key=lambda x: x[0]):
                access_type = "📝" if is_write else "👁️"
                lines.append(f"  {access_type} Line {line_num}:{col}")
                # Show first line of context only
                context_line = text.split("\n")[0].strip()
                if len(context_line) > 80:
                    context_line = context_line[:77] + "..."
                lines.append(f"     {context_line}")
            lines.append("")

        return "\n".join(lines)
    except PyCharmBridgeError as e:
        return f"Error: {e.message}\n{e.details or ''}"
    finally:
        await client.close()
