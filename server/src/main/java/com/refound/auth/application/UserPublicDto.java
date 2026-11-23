package com.refound.auth.application;

import java.util.UUID;

/** Thứ người khác được thấy về một người dùng: tên, ảnh, uy tín (03 §3.2, X17). DTO riêng để không lộ nhầm trường. */
public record UserPublicDto(UUID id, String displayName, String avatarUrl, int reputation) {}
