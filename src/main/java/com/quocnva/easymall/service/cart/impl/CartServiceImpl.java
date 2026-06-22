package com.quocnva.easymall.service.cart.impl;

import com.quocnva.easymall.dtos.request.cart.CartItemRequest;
import com.quocnva.easymall.dtos.response.cart.CartItemResponse;
import com.quocnva.easymall.dtos.response.cart.CartResponse;
import com.quocnva.easymall.entity.CartEntity;
import com.quocnva.easymall.entity.CartItemEntity;
import com.quocnva.easymall.entity.ProductVariantEntity;
import com.quocnva.easymall.entity.UserEntity;
import com.quocnva.easymall.exception.AppException;
import com.quocnva.easymall.exception.ErrorCode;
import com.quocnva.easymall.mapper.CartMapper;
import com.quocnva.easymall.repository.CartItemRepository;
import com.quocnva.easymall.repository.CartRepository;
import com.quocnva.easymall.repository.ProductVariantRepository;
import com.quocnva.easymall.repository.UserRepository;
import com.quocnva.easymall.service.cart.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;

    // ══════════════════════════════════════════════════════════════════
    // GET CART
    // ══════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(String email) {
        UserEntity user = resolveUser(email);
        CartEntity cart = getOrCreateCart(user);
        return buildCartResponse(cart);
    }

    // ══════════════════════════════════════════════════════════════════
    // ADD ITEM
    // ══════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public CartResponse addItem(String email, CartItemRequest request) {
        UserEntity user = resolveUser(email);
        CartEntity cart = getOrCreateCart(user);

        ProductVariantEntity variant = resolveVariant(request.getVariantId());

        // 1. Check BANNED
        validateNotBanned(variant);

        // 2. Tính limit tồn kho
        int limit = calculateLimit(variant);

        // 3. Cộng dồn với quantity hiện tại trong giỏ (nếu đã có)
        Optional<CartItemEntity> existing = cartItemRepository
                .findByCart_CartIdAndVariant_VariantId(cart.getCartId(), variant.getVariantId());

        int currentQty = existing.map(CartItemEntity::getQuantity).orElse(0);
        int newTotalQty = currentQty + request.getQuantity();

        // 4. Validate giới hạn
        validateQuantityLimit(variant, newTotalQty, limit);

        if (existing.isPresent()) {
            CartItemEntity item = existing.get();
            item.setQuantity(newTotalQty);
            item.setTotalMoney(variant.getPrice().multiply(BigDecimal.valueOf(newTotalQty)));
            if (request.getNote() != null) item.setNote(request.getNote());
            cartItemRepository.save(item);
        } else {
            CartItemEntity newItem = CartItemEntity.builder()
                    .cart(cart)
                    .variant(variant)
                    .quantity(newTotalQty)
                    .totalMoney(variant.getPrice().multiply(BigDecimal.valueOf(newTotalQty)))
                    .note(request.getNote())
                    .build();
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        // Reload để có data mới nhất
        CartEntity refreshed = cartRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));
        return buildCartResponse(refreshed);
    }

    // ══════════════════════════════════════════════════════════════════
    // UPDATE ITEM
    // ══════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public CartResponse updateItem(String email, Long variantId, CartItemRequest request) {
        UserEntity user = resolveUser(email);
        CartEntity cart = getOrCreateCart(user);

        CartItemEntity item = cartItemRepository
                .findByCart_CartIdAndVariant_VariantId(cart.getCartId(), variantId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        ProductVariantEntity variant = item.getVariant();

        // 1. Check BANNED
        validateNotBanned(variant);

        // 2. Tính limit
        int limit = calculateLimit(variant);

        // 3. Validate số lượng mới (thay thế hoàn toàn)
        validateQuantityLimit(variant, request.getQuantity(), limit);

        item.setQuantity(request.getQuantity());
        item.setTotalMoney(variant.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
        if (request.getNote() != null) item.setNote(request.getNote());
        cartItemRepository.save(item);

        CartEntity refreshed = cartRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));
        return buildCartResponse(refreshed);
    }

    // ══════════════════════════════════════════════════════════════════
    // REMOVE ITEM
    // ══════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void removeItem(String email, Long variantId) {
        UserEntity user = resolveUser(email);
        CartEntity cart = cartRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        CartItemEntity item = cartItemRepository
                .findByCart_CartIdAndVariant_VariantId(cart.getCartId(), variantId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        cartItemRepository.delete(item);
    }

    // ══════════════════════════════════════════════════════════════════
    // CLEAR CART
    // ══════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void clearCart(String email) {
        UserEntity user = resolveUser(email);
        CartEntity cart = cartRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.CART_NOT_FOUND));

        cartItemRepository.deleteByCart_CartId(cart.getCartId());
    }

    // ══════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════

    /**
     * Lookup user từ email (principal trong SecurityContext).
     */
    private UserEntity resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * Lấy cart của user, tự tạo mới nếu chưa có.
     */
    private CartEntity getOrCreateCart(UserEntity user) {
        return cartRepository.findByUser_UserId(user.getUserId())
                .orElseGet(() -> cartRepository.save(
                        CartEntity.builder()
                                .user(user)
                                .isActive(true)
                                .build()
                ));
    }

    /**
     * Lookup variant, throw nếu không tìm thấy.
     */
    private ProductVariantEntity resolveVariant(Long variantId) {
        return productVariantRepository.findById(variantId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
    }

    /**
     * Kiểm tra variant có bị khóa không.
     * isActive = false → PRODUCT_BANNED.
     */
    private void validateNotBanned(ProductVariantEntity variant) {
        if (Boolean.FALSE.equals(variant.getIsActive())) {
            throw new AppException(ErrorCode.PRODUCT_BANNED);
        }
    }

    /**
     * Tính giới hạn mua tối đa theo nghiệp vụ tồn kho:
     * - stock = -1 → limit = 99
     * - stock >= 0 → limit = stock - lockedStock
     * - maxOrderQuantity > 0 → limit = min(limit, maxOrderQuantity)
     */
    private int calculateLimit(ProductVariantEntity variant) {
        int limit;
        Integer stock = variant.getStockQuantity();
        Integer locked = variant.getLockedStock() != null ? variant.getLockedStock() : 0;

        if (stock != null && stock == -1) {
            limit = 99;
        } else {
            int available = (stock != null ? stock : 0) - locked;
            limit = Math.max(available, 0);
        }

        // Áp dụng maxOrderQuantity từ product nếu > 0
        Integer maxOrderQty = variant.getProduct().getMaxOrderQuantity();
        if (maxOrderQty != null && maxOrderQty > 0) {
            limit = Math.min(limit, maxOrderQty);
        }

        return limit;
    }

    /**
     * Validate số lượng yêu cầu không vượt quá giới hạn.
     * Phân biệt lỗi do kho hay do giới hạn mua.
     */
    private void validateQuantityLimit(ProductVariantEntity variant, int requestedQty, int limit) {
        if (requestedQty > limit) {
            Integer maxOrderQty = variant.getProduct().getMaxOrderQuantity();

            // Nếu limit bị thu hẹp bởi maxOrderQuantity → lỗi giới hạn mua
            if (maxOrderQty != null && maxOrderQty > 0 && requestedQty > maxOrderQty) {
                throw new AppException(ErrorCode.MAX_ORDER_QUANTITY_EXCEEDED);
            }
            // Còn lại → lỗi kho không đủ
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }

    /**
     * Xây dựng CartResponse từ CartEntity.
     * Tính toán availability cho từng item và tổng tiền của giỏ.
     */
    private CartResponse buildCartResponse(CartEntity cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> {
                    CartItemResponse dto = cartMapper.toCartItemResponse(item);
                    ProductVariantEntity variant = item.getVariant();

                    // Xác định trạng thái availability
                    if (Boolean.FALSE.equals(variant.getIsActive())) {
                        dto.setAvailable(false);
                        dto.setUnavailableReason("BANNED");
                    } else {
                        Integer stock = variant.getStockQuantity();
                        Integer locked = variant.getLockedStock() != null ? variant.getLockedStock() : 0;
                        boolean outOfStock = (stock != null && stock != -1 && (stock - locked) <= 0);
                        if (outOfStock) {
                            dto.setAvailable(false);
                            dto.setUnavailableReason("OUT_OF_STOCK");
                        } else {
                            dto.setAvailable(true);
                            dto.setUnavailableReason(null);
                        }
                    }
                    return dto;
                })
                .toList();

        // Tổng tiền chỉ từ các item hợp lệ
        BigDecimal totalAmount = itemResponses.stream()
                .filter(CartItemResponse::getAvailable)
                .map(CartItemResponse::getTotalMoney)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getCartId())
                .totalAmount(totalAmount)
                .items(itemResponses)
                .build();
    }
}
