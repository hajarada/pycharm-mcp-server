"""PyCharm Refactoring MCP Server."""

import asyncio

from mcp.server.fastmcp import FastMCP

from pycharm_mcp.tools import (
    change_signature,
    extract_method,
    extract_variable,
    find_usages,
    inline_element,
    list_projects,
    move_element,
    rename_symbol,
    safe_delete,
)

# Create the MCP server
mcp = FastMCP(
    "PyCharm Refactoring",
    description="Exposes PyCharm's refactoring capabilities for intelligent code transformations",
)


# Register all tools
@mcp.tool()
async def pycharm_list_projects() -> str:
    """
    List all projects currently open in PyCharm.

    Returns project names, paths, and which one is currently active.
    Use this to identify the correct project path before performing refactoring operations.

    Returns:
        A formatted list of open projects with their paths.
    """
    return await list_projects()


@mcp.tool()
async def pycharm_rename_symbol(
    project_path: str,
    file_path: str,
    line: int,
    column: int,
    new_name: str,
    search_in_comments: bool = True,
    search_in_strings: bool = False,
    preview: bool = False,
) -> str:
    """
    Rename a symbol (variable, function, class, etc.) across the entire project.

    Uses PyCharm's semantic rename which understands scope and updates all references.
    Files are auto-saved before refactoring.

    Args:
        project_path: Absolute path to the project (use pycharm_list_projects to find available projects)
        file_path: Path to the file containing the symbol (relative to project or absolute)
        line: Line number where the symbol is located (1-indexed)
        column: Column number where the symbol is located (1-indexed)
        new_name: The new name for the symbol
        search_in_comments: Also rename occurrences in comments (default: True)
        search_in_strings: Also rename occurrences in string literals (default: False)
        preview: If True, show what would change without applying (default: False)

    Returns:
        Summary of the rename operation including files modified and usages updated.
    """
    return await rename_symbol(
        project_path=project_path,
        file_path=file_path,
        line=line,
        column=column,
        new_name=new_name,
        search_in_comments=search_in_comments,
        search_in_strings=search_in_strings,
        preview=preview,
    )


@mcp.tool()
async def pycharm_move_element(
    project_path: str,
    file_path: str,
    line: int,
    column: int,
    target_file: str,
    preview: bool = False,
) -> str:
    """
    Move a class, function, or variable to a different module.

    Automatically updates all imports across the project. The element at the
    specified position will be moved to the target file.

    Args:
        project_path: Absolute path to the project
        file_path: Path to the source file containing the element
        line: Line number where the element is defined (1-indexed)
        column: Column number (1-indexed)
        target_file: Path to the destination file
        preview: If True, show what would change without applying (default: False)

    Returns:
        Summary of the move operation including import updates.
    """
    return await move_element(
        project_path=project_path,
        file_path=file_path,
        line=line,
        column=column,
        target_file=target_file,
        preview=preview,
    )


@mcp.tool()
async def pycharm_extract_method(
    project_path: str,
    file_path: str,
    start_line: int,
    start_column: int,
    end_line: int,
    end_column: int,
    method_name: str,
    preview: bool = False,
) -> str:
    """
    Extract selected code into a new method.

    PyCharm automatically determines parameters and return values based on the
    selected code's dependencies.

    Args:
        project_path: Absolute path to the project
        file_path: Path to the file containing the code to extract
        start_line: Starting line of the selection (1-indexed)
        start_column: Starting column of the selection (1-indexed)
        end_line: Ending line of the selection (1-indexed)
        end_column: Ending column of the selection (1-indexed)
        method_name: Name for the new method
        preview: If True, show what would change without applying (default: False)

    Returns:
        Summary of the extraction including the new method's signature.
    """
    return await extract_method(
        project_path=project_path,
        file_path=file_path,
        start_line=start_line,
        start_column=start_column,
        end_line=end_line,
        end_column=end_column,
        method_name=method_name,
        preview=preview,
    )


@mcp.tool()
async def pycharm_extract_variable(
    project_path: str,
    file_path: str,
    start_line: int,
    start_column: int,
    end_line: int,
    end_column: int,
    variable_name: str,
    replace_all: bool = True,
    preview: bool = False,
) -> str:
    """
    Extract an expression into a variable.

    The selected expression will be assigned to a new variable, and optionally
    all identical occurrences will be replaced.

    Args:
        project_path: Absolute path to the project
        file_path: Path to the file containing the expression
        start_line: Starting line of the expression (1-indexed)
        start_column: Starting column of the expression (1-indexed)
        end_line: Ending line of the expression (1-indexed)
        end_column: Ending column of the expression (1-indexed)
        variable_name: Name for the new variable
        replace_all: Replace all identical occurrences (default: True)
        preview: If True, show what would change without applying (default: False)

    Returns:
        Summary of the extraction including occurrences replaced.
    """
    return await extract_variable(
        project_path=project_path,
        file_path=file_path,
        start_line=start_line,
        start_column=start_column,
        end_line=end_line,
        end_column=end_column,
        variable_name=variable_name,
        replace_all=replace_all,
        preview=preview,
    )


@mcp.tool()
async def pycharm_inline_element(
    project_path: str,
    file_path: str,
    line: int,
    column: int,
    preview: bool = False,
) -> str:
    """
    Inline a variable or method (replace usages with the definition).

    For variables, each usage is replaced with the assigned value.
    For methods, each call is replaced with the method body.

    Args:
        project_path: Absolute path to the project
        file_path: Path to the file containing the element
        line: Line number where the element is defined (1-indexed)
        column: Column number (1-indexed)
        preview: If True, show what would change without applying (default: False)

    Returns:
        Summary of the inline operation including usages replaced.
    """
    return await inline_element(
        project_path=project_path,
        file_path=file_path,
        line=line,
        column=column,
        preview=preview,
    )


@mcp.tool()
async def pycharm_change_signature(
    project_path: str,
    file_path: str,
    line: int,
    column: int,
    new_name: str | None = None,
    parameters: list[dict[str, str | None]] | None = None,
    return_type: str | None = None,
    preview: bool = False,
) -> str:
    """
    Change a function's signature (name, parameters, return type).

    Updates all call sites automatically. You can change the function name,
    add/remove/reorder parameters, and change the return type annotation.

    Args:
        project_path: Absolute path to the project
        file_path: Path to the file containing the function
        line: Line number where the function is defined (1-indexed)
        column: Column number (1-indexed)
        new_name: New name for the function (optional)
        parameters: New parameter list, each with 'name', optional 'type', and optional 'defaultValue'
        return_type: New return type annotation (optional)
        preview: If True, show what would change without applying (default: False)

    Returns:
        Summary of the signature change including call sites updated.
    """
    return await change_signature(
        project_path=project_path,
        file_path=file_path,
        line=line,
        column=column,
        new_name=new_name,
        parameters=parameters,
        return_type=return_type,
        preview=preview,
    )


@mcp.tool()
async def pycharm_safe_delete(
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
    return await safe_delete(
        project_path=project_path,
        file_path=file_path,
        line=line,
        column=column,
        search_for_usages=search_for_usages,
    )


@mcp.tool()
async def pycharm_find_usages(
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
    return await find_usages(
        project_path=project_path,
        file_path=file_path,
        line=line,
        column=column,
    )


def main() -> None:
    """Run the MCP server."""
    mcp.run()


if __name__ == "__main__":
    main()
