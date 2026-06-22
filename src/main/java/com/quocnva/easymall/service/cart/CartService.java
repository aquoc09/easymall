package com.quocnva.easymall.service.cart;

import com.quocnva.easymall.dtos.request.cart.CartItemRequest;
import com.quocnva.easymall.dtos.response.cart.CartResponse;

public interface CartService {

    /**
     * Lấy giỏ hàng của user hiện tại.
     * Tự tạo mới nếu user chưa có cart.
     */
    CartResponse getCart(String email);

    /**
     * Thêm item vào giỏ. Nếu item đã tồn tại thì cộng dồn số lượng.
     * Thứ tự kiểm tra: BANNED → Inventory type → Max limit → Save.
     */
    CartResponse addItem(String email, CartItemRequest request);

    /**
     * Cập nhật số lượng của 1 item (thay thế hoàn toàn, không cộng dồn).
     */
    CartResponse updateItem(String email, Long variantId, CartItemRequest request);

    /**
     * Xóa 1 item khỏi giỏ theo variantId.
     */
    void removeItem(String email, Long variantId);

    /**
     * Xóa toàn bộ items trong giỏ hàng.
     */
    void clearCart(String email);
}
