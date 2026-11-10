package ru.skinemsya.expense;

/**
 * Как трата делится между участниками.
 */
public enum SplitType {
    /** Поровну. */
    EQUAL,
    /** Точными суммами. */
    EXACT,
    /** Пропорционально долям, например 2:1:1. */
    SHARES
}
