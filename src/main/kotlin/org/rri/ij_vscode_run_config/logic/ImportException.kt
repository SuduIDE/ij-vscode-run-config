package org.rri.ij_vscode_run_config.logic

import java.util.LinkedList

abstract class ImportException(message: String, cause: Throwable? = null) : Throwable(message, cause)

class ImportError(message: String, cause: Throwable? = null) : ImportException(message, cause)

class ImportWarning(message: String, cause: Throwable? = null) : ImportException(message, cause)

class ImportProblems {
    private val errors: MutableSet<Throwable> = HashSet()
    private val warnings: MutableSet<Throwable> = HashSet()

    fun addError(exc: Throwable) {
        errors.add(exc)
    }

    fun addWarning(exc: Throwable) {
        warnings.add(exc)
    }

    fun getErrors(): Set<Throwable> = errors

    fun getWarnings(): Set<Throwable>  = warnings

    fun isEmpty(): Boolean {
        return errors.isEmpty() && warnings.isEmpty()
    }
}
