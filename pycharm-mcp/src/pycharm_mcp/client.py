"""HTTP client for communicating with the PyCharm Refactoring Bridge plugin."""

import os
from typing import Any

import httpx

from pycharm_mcp.models import (
    ChangeSignatureResponse,
    ExtractMethodResponse,
    ExtractVariableResponse,
    FindUsagesResponse,
    HealthResponse,
    InlineResponse,
    MoveResponse,
    ParameterInfo,
    ProjectListResponse,
    RenameResponse,
    SafeDeleteResponse,
)


class PyCharmBridgeError(Exception):
    """Error communicating with PyCharm Bridge."""

    def __init__(self, message: str, details: str | None = None) -> None:
        self.message = message
        self.details = details
        super().__init__(f"{message}: {details}" if details else message)


class PyCharmClient:
    """Client for the PyCharm Refactoring Bridge HTTP API."""

    def __init__(
        self,
        base_url: str | None = None,
        auth_token: str | None = None,
        timeout: float = 30.0,
    ) -> None:
        self.base_url = base_url or os.environ.get(
            "PYCHARM_BRIDGE_URL", "http://localhost:9876"
        )
        self.auth_token = auth_token or os.environ.get("PYCHARM_BRIDGE_TOKEN")
        self.timeout = timeout
        self._client: httpx.AsyncClient | None = None

    async def _get_client(self) -> httpx.AsyncClient:
        if self._client is None or self._client.is_closed:
            headers: dict[str, str] = {"Content-Type": "application/json"}
            if self.auth_token:
                headers["Authorization"] = f"Bearer {self.auth_token}"
            self._client = httpx.AsyncClient(
                base_url=self.base_url,
                headers=headers,
                timeout=self.timeout,
            )
        return self._client

    async def close(self) -> None:
        """Close the HTTP client."""
        if self._client is not None:
            await self._client.aclose()
            self._client = None

    async def _request(
        self, method: str, path: str, json_data: dict[str, Any] | None = None
    ) -> dict[str, Any]:
        """Make an HTTP request to the PyCharm bridge."""
        client = await self._get_client()
        try:
            if method == "GET":
                response = await client.get(path)
            else:
                response = await client.post(path, json=json_data)

            response.raise_for_status()
            data: dict[str, Any] = response.json()

            if not data.get("success", True):
                raise PyCharmBridgeError(
                    data.get("error", "Unknown error"),
                    data.get("details"),
                )

            return data

        except httpx.ConnectError as e:
            raise PyCharmBridgeError(
                "Cannot connect to PyCharm",
                "Is PyCharm running with the Refactoring Bridge plugin installed?",
            ) from e
        except httpx.HTTPStatusError as e:
            error_data = e.response.json() if e.response.content else {}
            raise PyCharmBridgeError(
                error_data.get("error", f"HTTP {e.response.status_code}"),
                error_data.get("details"),
            ) from e

    async def health(self) -> HealthResponse:
        """Check if the PyCharm bridge is healthy."""
        data = await self._request("GET", "/health")
        return HealthResponse.model_validate(data)

    async def list_projects(self) -> ProjectListResponse:
        """List all projects currently open in PyCharm."""
        data = await self._request("GET", "/projects")
        return ProjectListResponse.model_validate(data)

    async def rename(
        self,
        project: str,
        file: str,
        line: int,
        column: int,
        new_name: str,
        search_in_comments: bool = True,
        search_in_strings: bool = False,
        preview: bool = False,
    ) -> RenameResponse:
        """Rename a symbol."""
        data = await self._request(
            "POST",
            "/refactor/rename",
            {
                "project": project,
                "file": file,
                "line": line,
                "column": column,
                "newName": new_name,
                "searchInComments": search_in_comments,
                "searchInStrings": search_in_strings,
                "preview": preview,
            },
        )
        return RenameResponse.model_validate(data)

    async def move(
        self,
        project: str,
        file: str,
        line: int,
        column: int,
        target_file: str,
        preview: bool = False,
    ) -> MoveResponse:
        """Move an element to a different module."""
        data = await self._request(
            "POST",
            "/refactor/move",
            {
                "project": project,
                "file": file,
                "line": line,
                "column": column,
                "targetFile": target_file,
                "preview": preview,
            },
        )
        return MoveResponse.model_validate(data)

    async def extract_method(
        self,
        project: str,
        file: str,
        start_line: int,
        start_column: int,
        end_line: int,
        end_column: int,
        method_name: str,
        preview: bool = False,
    ) -> ExtractMethodResponse:
        """Extract selected code into a new method."""
        data = await self._request(
            "POST",
            "/refactor/extract-method",
            {
                "project": project,
                "file": file,
                "startLine": start_line,
                "startColumn": start_column,
                "endLine": end_line,
                "endColumn": end_column,
                "methodName": method_name,
                "preview": preview,
            },
        )
        return ExtractMethodResponse.model_validate(data)

    async def extract_variable(
        self,
        project: str,
        file: str,
        start_line: int,
        start_column: int,
        end_line: int,
        end_column: int,
        variable_name: str,
        replace_all: bool = True,
        preview: bool = False,
    ) -> ExtractVariableResponse:
        """Extract an expression into a variable."""
        data = await self._request(
            "POST",
            "/refactor/extract-variable",
            {
                "project": project,
                "file": file,
                "startLine": start_line,
                "startColumn": start_column,
                "endLine": end_line,
                "endColumn": end_column,
                "variableName": variable_name,
                "replaceAll": replace_all,
                "preview": preview,
            },
        )
        return ExtractVariableResponse.model_validate(data)

    async def inline(
        self,
        project: str,
        file: str,
        line: int,
        column: int,
        preview: bool = False,
    ) -> InlineResponse:
        """Inline a variable or method."""
        data = await self._request(
            "POST",
            "/refactor/inline",
            {
                "project": project,
                "file": file,
                "line": line,
                "column": column,
                "preview": preview,
            },
        )
        return InlineResponse.model_validate(data)

    async def change_signature(
        self,
        project: str,
        file: str,
        line: int,
        column: int,
        new_name: str | None = None,
        parameters: list[ParameterInfo] | None = None,
        return_type: str | None = None,
        preview: bool = False,
    ) -> ChangeSignatureResponse:
        """Change a function's signature."""
        request_data: dict[str, Any] = {
            "project": project,
            "file": file,
            "line": line,
            "column": column,
            "preview": preview,
        }
        if new_name is not None:
            request_data["newName"] = new_name
        if parameters is not None:
            request_data["parameters"] = [p.model_dump(by_alias=True) for p in parameters]
        if return_type is not None:
            request_data["returnType"] = return_type

        data = await self._request("POST", "/refactor/change-signature", request_data)
        return ChangeSignatureResponse.model_validate(data)

    async def safe_delete(
        self,
        project: str,
        file: str,
        line: int,
        column: int,
        search_for_usages: bool = True,
    ) -> SafeDeleteResponse:
        """Delete an element only if it has no usages."""
        data = await self._request(
            "POST",
            "/refactor/safe-delete",
            {
                "project": project,
                "file": file,
                "line": line,
                "column": column,
                "searchForUsages": search_for_usages,
            },
        )
        return SafeDeleteResponse.model_validate(data)

    async def find_usages(
        self,
        project: str,
        file: str,
        line: int,
        column: int,
    ) -> FindUsagesResponse:
        """Find all usages of a symbol."""
        data = await self._request(
            "POST",
            "/find/usages",
            {
                "project": project,
                "file": file,
                "line": line,
                "column": column,
            },
        )
        return FindUsagesResponse.model_validate(data)
