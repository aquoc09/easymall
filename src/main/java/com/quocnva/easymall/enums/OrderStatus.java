package com.quocnva.easymall.enums;

public enum OrderStatus {
    /** Chờ xác nhận từ Shop */
    PENDING,
    /** Giao dịch có dấu hiệu gian lận, cần Admin duyệt tay */
    PENDING_REVIEW,
    /** Đang chờ thanh toán online (VNPAY / MoMo) — TTL 15 phút */
    PENDING_PAYMENT,
    /** Shop đã xác nhận, đang chờ GHN lấy hàng */
    AWAITING_SHIPMENT,
    /** GHN đã lấy hàng, đang vận chuyển */
    SHIPPING,
    /** Đã giao thành công */
    DELIVERED,
    /** Hoàn tất sau 72h (tiền giải phóng cho Shop) */
    COMPLETED,
    /** Đã hủy */
    CANCELLED,
    /** Hàng bị hoàn về kho */
    RETURNED,
    /** Hoàn tiền thất bại — cần Admin xử lý thủ công */
    REFUND_FAILED
}
