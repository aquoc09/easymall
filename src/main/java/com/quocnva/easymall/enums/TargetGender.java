package com.quocnva.easymall.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * TargetGender — giới tính mục tiêu của sản phẩm.
 * <p>
 * DB lưu SMALLINT: 0=Nữ, 1=Nam, 2=Unisex.
 * <p>
 * {@code @JsonCreator} hỗ trợ deserialize từ cả hai dạng:
 * <ul>
 *   <li>Số nguyên: {@code 0}, {@code 1}, {@code 2}</li>
 *   <li>Chuỗi:     {@code "FEMALE"}, {@code "MALE"}, {@code "UNISEX"}, {@code "OTHER"} → ánh xạ về UNISEX</li>
 * </ul>
 * {@code @JsonValue} đảm bảo response trả về số — không breaking frontend.
 */
public enum TargetGender {

    FEMALE(0),
    MALE(1),
    UNISEX(2);

    private final short code;

    TargetGender(int code) {
        this.code = (short) code;
    }

    /** Serialize → số nguyên (0 / 1 / 2) trong JSON response. */
    @JsonValue
    public short getCode() {
        return code;
    }

    /**
     * Deserialize từ JSON — chấp nhận cả số lẫn chuỗi.
     *
     * @param value Integer hoặc String từ JSON payload
     * @return TargetGender tương ứng; chuỗi không nhận diện được → UNISEX
     */
    @JsonCreator
    public static TargetGender fromValue(Object value) {
        if (value == null) return null;

        // Dạng số: 0, 1, 2
        if (value instanceof Number n) {
            return switch (n.intValue()) {
                case 0 -> FEMALE;
                case 1 -> MALE;
                default -> UNISEX;
            };
        }

        // Dạng chuỗi: "FEMALE", "MALE", "UNISEX", "OTHER", "0", "1", "2", ...
        return switch (value.toString().trim().toUpperCase()) {
            case "FEMALE", "NU", "0" -> FEMALE;
            case "MALE", "NAM", "1"  -> MALE;
            default                  -> UNISEX; // "UNISEX", "OTHER", "2", ...
        };
    }
}
