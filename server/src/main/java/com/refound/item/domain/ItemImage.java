package com.refound.item.domain;

import java.util.UUID;

/** Một dòng {@code item_images}: bản lớn 1024px và bản nhỏ 512px trong bucket private (ADR-06, ADR-14). */
public record ItemImage(UUID id, UUID itemId, int position, String fullPath, String thumbPath) {}
