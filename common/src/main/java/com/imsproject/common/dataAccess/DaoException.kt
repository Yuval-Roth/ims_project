package com.imsproject.common.dataAccess

/**
 * @apiNote  This exception does not create a stack trace.
 */
class DaoException : Exception {
    /**
     * Constructs a new exception with `null` as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to [.initCause].
     */
    constructor() : super(null, null, true, false)

    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to [.initCause].
     *
     * @param message the detail message. The detail message is saved for
     * later retrieval by the [.getMessage] method.
     */
    constructor(message: String?) : super(message, null, true, false)

    /**
     * Constructs a new exception with the specified detail message and
     * cause.
     *
     *Note that the detail message associated with
     * `cause` is *not* automatically incorporated in
     * this exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval
     * by the [.getMessage] method).
     * @param cause   the cause (which is saved for later retrieval by the
     * [.getCause] method).  (A `null` value is
     * permitted, and indicates that the cause is nonexistent or
     * unknown.)
     * @since 1.4
     */
    constructor(message: String?, cause: Throwable?) : super(message, cause, true, false)

    /**
     * Constructs a new exception with the specified cause and a detail
     * message of `(cause==null ? null : cause.toString())` (which
     * typically contains the class and detail message of `cause`).
     * This constructor is useful for exceptions that are little more than
     * wrappers for other throwables (for example, [ ]).
     *
     * @param cause the cause (which is saved for later retrieval by the
     * [.getCause] method).  (A `null` value is
     * permitted, and indicates that the cause is nonexistent or
     * unknown.)
     * @since 1.4
     */
    constructor(cause: Throwable?) : super(null, cause, true, false)
}
