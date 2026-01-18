"""Pydantic models for PyCharm MCP server."""

from typing import Optional

from pydantic import BaseModel, Field


class FileChange(BaseModel):
    """Represents a single file change from a refactoring operation."""

    file: str
    line: int
    old_text: str = Field(alias="oldText")
    new_text: str = Field(alias="newText")

    model_config = {"populate_by_name": True}


class ProjectInfo(BaseModel):
    """Information about an open project in PyCharm."""

    name: str
    path: str
    is_open: bool = Field(alias="isOpen")
    is_default: bool = Field(default=False, alias="isDefault")

    model_config = {"populate_by_name": True}


class UsageInfo(BaseModel):
    """Information about a symbol usage."""

    file: str
    line: int
    column: int
    text: str
    is_write_access: bool = Field(default=False, alias="isWriteAccess")

    model_config = {"populate_by_name": True}


class ParameterInfo(BaseModel):
    """Information about a function parameter."""

    name: str
    type: Optional[str] = None
    default_value: Optional[str] = Field(default=None, alias="defaultValue")

    model_config = {"populate_by_name": True}


# Response models


class ErrorResponse(BaseModel):
    """Error response from the PyCharm bridge."""

    success: bool = False
    error: str
    details: Optional[str] = None


class HealthResponse(BaseModel):
    """Health check response."""

    status: str
    version: str
    projects_open: int = Field(alias="projectsOpen")

    model_config = {"populate_by_name": True}


class ProjectListResponse(BaseModel):
    """Response containing list of open projects."""

    success: bool = True
    projects: list[ProjectInfo]


class RenameResponse(BaseModel):
    """Response from a rename operation."""

    success: bool = True
    changes: list[FileChange]
    files_modified: int = Field(alias="filesModified")
    usages_updated: int = Field(alias="usagesUpdated")

    model_config = {"populate_by_name": True}


class MoveResponse(BaseModel):
    """Response from a move operation."""

    success: bool = True
    changes: list[FileChange]
    files_modified: int = Field(alias="filesModified")
    imports_updated: int = Field(alias="importsUpdated")

    model_config = {"populate_by_name": True}


class ExtractMethodResponse(BaseModel):
    """Response from an extract method operation."""

    success: bool = True
    file: str
    method_line: int = Field(alias="methodLine")
    parameters: list[str]
    return_type: Optional[str] = Field(default=None, alias="returnType")

    model_config = {"populate_by_name": True}


class ExtractVariableResponse(BaseModel):
    """Response from an extract variable operation."""

    success: bool = True
    file: str
    variable_line: int = Field(alias="variableLine")
    occurrences_replaced: int = Field(alias="occurrencesReplaced")

    model_config = {"populate_by_name": True}


class InlineResponse(BaseModel):
    """Response from an inline operation."""

    success: bool = True
    changes: list[FileChange]
    usages_inlined: int = Field(alias="usagesInlined")

    model_config = {"populate_by_name": True}


class ChangeSignatureResponse(BaseModel):
    """Response from a change signature operation."""

    success: bool = True
    changes: list[FileChange]
    call_sites_updated: int = Field(alias="callSitesUpdated")

    model_config = {"populate_by_name": True}


class SafeDeleteResponse(BaseModel):
    """Response from a safe delete operation."""

    success: bool = True
    deleted: bool
    usages_found: int = Field(default=0, alias="usagesFound")
    usages: Optional[list[UsageInfo]] = None

    model_config = {"populate_by_name": True}


class FindUsagesResponse(BaseModel):
    """Response from a find usages operation."""

    success: bool = True
    symbol: str
    usages: list[UsageInfo]
    total_count: int = Field(alias="totalCount")

    model_config = {"populate_by_name": True}
