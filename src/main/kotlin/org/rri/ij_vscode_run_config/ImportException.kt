package org.rri.ij_vscode_run_config

abstract class ImportException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

class ImportError(message: String? = null, cause: Throwable? = null) : ImportException(message, cause)

class ImportWarning(message: String? = null, cause: Throwable? = null) : ImportException(message, cause)