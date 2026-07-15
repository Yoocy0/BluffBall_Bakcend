package com.project.bluffball.global.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.bluffball.global.exception.ErrorResponse;
import com.project.bluffball.global.exception.ErrorResponseResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

/**
 * STOMP ERROR 프레임을 공통 {@link ErrorResponse} JSON 형식으로 반환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomStompSubProtocolErrorHandler extends StompSubProtocolErrorHandler {

    private static final byte[] EMPTY_PAYLOAD = new byte[0];

    private final ObjectMapper objectMapper;
    private final ErrorResponseResolver errorResponseResolver;

    @Override
    @Nullable
    public Message<byte[]> handleClientMessageProcessingError(
            @Nullable Message<byte[]> clientMessage,
            Throwable ex) {
        Throwable cause = ex;
        log.debug("STOMP client message processing error", ex);

        ErrorResponse errorResponse = errorResponseResolver.resolve(cause);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(errorResponse.message());
        accessor.setLeaveMutable(true);
        copyReceiptId(clientMessage, accessor);

        byte[] payload = serialize(errorResponse);
        accessor.setContentType(MimeTypeUtils.APPLICATION_JSON);

        return MessageBuilder.createMessage(payload, accessor.getMessageHeaders());
    }

    private void copyReceiptId(@Nullable Message<byte[]> clientMessage, StompHeaderAccessor accessor) {
        if (clientMessage == null) {
            return;
        }
        StompHeaderAccessor clientAccessor =
                MessageHeaderAccessor.getAccessor(clientMessage, StompHeaderAccessor.class);
        if (clientAccessor != null && clientAccessor.getReceipt() != null) {
            accessor.setReceiptId(clientAccessor.getReceipt());
        }
    }

    private byte[] serialize(ErrorResponse errorResponse) {
        try {
            return objectMapper.writeValueAsBytes(errorResponse);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize STOMP error response", ex);
            return EMPTY_PAYLOAD;
        }
    }
}
