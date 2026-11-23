package com.refound.common;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** Máy trạng thái từ chối một chuyển trạng thái (05): API trả 409 {@code INVALID_STATE}. */
public class InvalidTransitionException extends ApiException {

    public InvalidTransitionException(Enum<?> from, Enum<?> action) {
        super(HttpStatus.CONFLICT, "INVALID_STATE", "Không thể thực hiện thao tác này ở trạng thái hiện tại.",
                Map.of("from", from.name().toLowerCase(), "action", action.name().toLowerCase()));
    }
}
