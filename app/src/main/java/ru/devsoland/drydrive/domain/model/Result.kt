package ru.devsoland.drydrive.domain.model

/**
 * Общий класс для представления результата операции, который может быть либо успешным (Success),
 * либо содержать ошибку (Error).
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>()
}
