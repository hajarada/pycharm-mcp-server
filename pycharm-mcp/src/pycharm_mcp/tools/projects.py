"""Tool for listing open projects in PyCharm."""

from pycharm_mcp.client import PyCharmBridgeError, PyCharmClient


async def list_projects() -> str:
    """
    List all projects currently open in PyCharm.

    Returns project names, paths, and which one is currently active.
    Use this to identify the correct project path before performing refactoring operations.

    Returns:
        A formatted list of open projects with their paths.
    """
    client = PyCharmClient()
    try:
        response = await client.list_projects()

        if not response.projects:
            return "No projects are currently open in PyCharm."

        lines = ["Open projects in PyCharm:", ""]
        for project in response.projects:
            status = " (default)" if project.is_default else ""
            lines.append(f"  • {project.name}{status}")
            lines.append(f"    Path: {project.path}")
            lines.append("")

        return "\n".join(lines)
    except PyCharmBridgeError as e:
        return f"Error: {e.message}\n{e.details or ''}"
    finally:
        await client.close()
