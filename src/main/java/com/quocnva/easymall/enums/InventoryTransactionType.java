package com.quocnva.easymall.enums;

public enum InventoryTransactionType {
    /** Nhập kho từ nhà cung cấp / phiếu nhập thủ công */
    IMPORT,
    /** Xuất kho do đơn hàng */
    ORDER,
    /** Hoàn trả hàng từ khách */
    RETURN,
    /** Điều chỉnh tồn kho thủ công */
    ADJUSTMENT
}
