"""Tool for safe deletion in PyCharm."""

from pycharm_mcp.client import PyCharmBridgeError, PyCharmClient


async def safe_delete(
    project_path: str,
    file_path: str,
    line: int,
    column: int,
    search_for_usages: bool = True,
) -> str:
    """
    Delete an element only if it has no usages.

    This performs a "safe" delete that checks for usages before removing the
    element. If usages are found, the delete is aborted and the usages are
    reported.

    Args:
        project_path: Absolute path to the project
        file_path: Path to the file containing the element
        line: Line number where the element is defined (1-indexed)
        column: Column number (1-indexed)
        search_for_usages: Check for usages before deleting (default: True)

    Returns:
        Confirmation of deletion or list of usages that prevent deletion.
    """
    client = PyCharmClient()
    try:
        response = await client.safe_delete(
            project=project_path,
            file=file_path,
            line=line,
            column=column,
            search_for_usages=search_for_usages,
        )

        if response.deleted:
            return "Successfully deleted element (no usages found)."

        lines = [f"Cannot delete: {response.usages_found} usage(s) found:", ""]

        if response.usages:
            for usage in response.usages[:15]:
                access_type = "write" if usage.is_write_access else "read"
                lines.append(f"  • {usage.file}:{usage.line}:{usage.column} ({access_type})")
                lines.append(f"    {usage.text}")
                lines.append("")

            if len(response.usages) > 15:
                lines.append(f"  ... and {len(response.usages) - 15} more usages")

        lines.append("")
        lines.append("Remove or update these usages before deleting.")

        return "\n".join(lines)
    except PyCharmBridgeError as e:
        return f"Error: {e.message}\n{e.details or ''}"
    finally:
        await client.close()
