package com.project.bluffball.global.exception;

import org.springframework.stereotype.Component;

/**
 * 예외를 공통 {@link ErrorResponse}로 변환한다.
 */
@Component
public class ErrorResponseResolver {

    public ErrorResponse resolve(Throwable ex) {
        Throwable cause = unwrap(ex);
        if (cause instanceof CustomException customException) {
            return ErrorResponse.from(customException);
        }
        return ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    private Throwable unwrap(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && isWrapper(current)) {
            current = current.getCause();
        }
        return current;
    }

    private boolean isWrapper(Throwable ex) {
        return ex instanceof org.springframework.messaging.MessagingException
                || ex instanceof java.util.concurrent.ExecutionException
                || ex instanceof java.lang.reflect.InvocationTargetException;
    }
}
