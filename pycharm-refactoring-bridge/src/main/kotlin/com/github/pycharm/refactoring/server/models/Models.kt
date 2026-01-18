package com.github.pycharm.refactoring.server.models

import kotlinx.serialization.Serializable

// ========== Common Models ==========

@Serializable
data class ErrorResponse(
    val success: Boolean = false,
    val error: String,
    val details: String? = null
)

@Serializable
data class FileChange(
    val file: String,
    val line: Int,
    val oldText: String,
    val newText: String
)

// ========== Project Models ==========

@Serializable
data class ProjectInfo(
    val name: String,
    val path: String,
    val isOpen: Boolean,
    val isDefault: Boolean = false
)

@Serializable
data class ProjectListResponse(
    val success: Boolean = true,
    val projects: List<ProjectInfo>
)

// ========== Rename Models ==========

@Serializable
data class RenameRequest(
    val project: String,
    val file: String,
    val line: Int,
    val column: Int,
    val newName: String,
    val searchInComments: Boolean = true,
    val searchInStrings: Boolean = false,
    val preview: Boolean = false
)

@Serializable
data class RenameResponse(
    val success: Boolean = true,
    val changes: List<FileChange>,
    val filesModified: Int,
    val usagesUpdated: Int
)

// ========== Move Models ==========

@Serializable
data class MoveRequest(
    val project: String,
    val file: String,
    val line: Int,
    val column: Int,
    val targetFile: String,
    val preview: Boolean = false
)

@Serializable
data class MoveResponse(
    val success: Boolean = true,
    val changes: List<FileChange>,
    val filesModified: Int,
    val importsUpdated: Int
)

// ========== Extract Models ==========

@Serializable
data class ExtractMethodRequest(
    val project: String,
    val file: String,
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
    val methodName: String,
    val preview: Boolean = false
)

@Serializable
data class ExtractMethodResponse(
    val success: Boolean = true,
    val file: String,
    val methodLine: Int,
    val parameters: List<String>,
    val returnType: String?
)

@Serializable
data class ExtractVariableRequest(
    val project: String,
    val file: String,
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
    val variableName: String,
    val replaceAll: Boolean = true,
    val preview: Boolean = false
)

@Serializable
data class ExtractVariableResponse(
    val success: Boolean = true,
    val file: String,
    val variableLine: Int,
    val occurrencesReplaced: Int
)

// ========== Inline Models ==========

@Serializable
data class InlineRequest(
    val project: String,
    val file: String,
    val line: Int,
    val column: Int,
    val preview: Boolean = false
)

@Serializable
data class InlineResponse(
    val success: Boolean = true,
    val changes: List<FileChange>,
    val usagesInlined: Int
)

// ========== Change Signature Models ==========

@Serializable
data class ParameterInfo(
    val name: String,
    val type: String? = null,
    val defaultValue: String? = null
)

@Serializable
data class ChangeSignatureRequest(
    val project: String,
    val file: String,
    val line: Int,
    val column: Int,
    val newName: String? = null,
    val parameters: List<ParameterInfo>? = null,
    val returnType: String? = null,
    val preview: Boolean = false
)

@Serializable
data class ChangeSignatureResponse(
    val success: Boolean = true,
    val changes: List<FileChange>,
    val callSitesUpdated: Int
)

// ========== Safe Delete Models ==========

@Serializable
data class SafeDeleteRequest(
    val project: String,
    val file: String,
    val line: Int,
    val column: Int,
    val searchForUsages: Boolean = true
)

@Serializable
data class SafeDeleteResponse(
    val success: Boolean = true,
    val deleted: Boolean,
    val usagesFound: Int = 0,
    val usages: List<UsageInfo>? = null
)

// ========== Find Usages Models ==========

@Serializable
data class FindUsagesRequest(
    val project: String,
    val file: String,
    val line: Int,
    val column: Int
)

@Serializable
data class UsageInfo(
    val file: String,
    val line: Int,
    val column: Int,
    val text: String,
    val isWriteAccess: Boolean = false
)

@Serializable
data class FindUsagesResponse(
    val success: Boolean = true,
    val symbol: String,
    val usages: List<UsageInfo>,
    val totalCount: Int
)

// ========== Health Check ==========

@Serializable
data class HealthResponse(
    val status: String = "ok",
    val version: String,
    val projectsOpen: Int
)
