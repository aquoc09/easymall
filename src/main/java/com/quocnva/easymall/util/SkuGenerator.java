package com.quocnva.easymall.util;

import java.util.Map;

/**
 * Utility tạo SKU code theo cấu trúc: [CATEGORY_CODE]-[PRODUCT_ID]-[ATTR1]-[ATTR2]-...
 *
 * <p>Công thức: {@code [CATEGORY]-[PRODUCT_ID]-[ATTRIBUTES]}
 * Ví dụ: AP-1-NVY-M, SH-2-WHT-42
 *
 * <p>Quy tắc:
 * <ul>
 *   <li>Luôn viết HOA (toUpperCase)</li>
 *   <li>Không dùng O/0 và I/l (tránh lẫn lộn)</li>
 *   <li>Tổng độ dài không quá 50 ký tự (column constraint)</li>
 * </ul>
 */
public class SkuGenerator {

    private SkuGenerator() {
        // Utility class
    }

    /**
     * Sinh SKU từ category code, product ID, và map attribute (thứ tự nhập vào).
     *
     * @param categoryCode mã danh mục, ví dụ "AP", "SH", "AT"
     * @param productId    ID sản phẩm
     * @param attributes   Map<String, String> — các thuộc tính biến thể theo thứ tự nhập
     *                     (LinkedHashMap giữ thứ tự), ví dụ {color=NVY, size=M}
     * @return SKU dạng "AP-1-NVY-M" (uppercase, tối đa 50 ký tự)
     */
    public static String generate(String categoryCode, Long productId, Map<String, String> attributes) {
        StringBuilder sb = new StringBuilder();

        // [CATEGORY]
        sb.append(sanitize(categoryCode));
        // [PRODUCT_ID]
        sb.append("-").append(productId);

        // [ATTRIBUTES] — lấy values theo thứ tự của map
        if (attributes != null) {
            for (String value : attributes.values()) {
                sb.append("-").append(sanitize(value));
            }
        }

        String sku = sb.toString().toUpperCase();

        // Guard: cắt nếu dài hơn 50 ký tự (column length limit)
        if (sku.length() > 50) {
            sku = sku.substring(0, 50);
        }

        return sku;
    }

    /**
     * Loại bỏ ký tự không hợp lệ, chỉ giữ chữ-số-gạch ngang.
     */
    private static String sanitize(String input) {
        if (input == null || input.isBlank()) return "";
        return input.trim().replaceAll("[^A-Za-z0-9]", "");
    }
}
