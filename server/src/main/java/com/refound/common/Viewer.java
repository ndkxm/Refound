package com.refound.common;

import java.util.UUID;

/** Người gọi API: id từ JWT, {@code admin} từ quyền đọc trong DB ở mỗi request (UserStatusFilter), không từ claim JWT. */
public record Viewer(UUID id, boolean admin) {

    public boolean is(UUID userId) {
        return id.equals(userId);
    }
}
