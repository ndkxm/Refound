package com.refound.item.domain;

/** Một dòng {@code categories} (dữ liệu cố định nạp trong migration P0). */
public record Category(String code, String nameVi, String groupCode, boolean sensitive) {}
