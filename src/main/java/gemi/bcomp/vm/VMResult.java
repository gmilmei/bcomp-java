package gemi.bcomp.vm;

public enum VMResult {

    OK,
    HALTED,
    NOT_YET_IMPLEMENTED,
    ILLEGAL_INSTRUCTION,
    ILLEGAL_MEMORY_ACCESS,
    DIVISION_BY_ZERO,
    UNDEFINED_SYSTEM_CALL,
    UNSUPPORTED_SYSTEM_CALL
}
