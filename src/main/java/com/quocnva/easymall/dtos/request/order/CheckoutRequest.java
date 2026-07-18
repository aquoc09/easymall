package com.quocnva.easymall.dtos.request.order;

import com.quocnva.easymall.enums.PaymentMethod;
import com.quocnva.easymall.enums.ShippingMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CheckoutRequest {

    @NotEmpty(message = "{validation.cartItemIds.not-empty}")
    private List<Long> cartItemIds;

    @NotNull(message = "{validation.addressId.not-null}")
    private Long addressId;

    /** Mã coupon — optional. Client có thể truyền nhiều mã cùng lúc (Vd: Shop + FreeShip) */
    private List<String> couponCodes;

    @NotNull(message = "{validation.paymentMethod.not-null}")
    private PaymentMethod paymentMethod;

    @NotNull(message = "{validation.shippingMethod.not-null}")
    private ShippingMethod shippingMethod;

    @Size(max = 200, message = "{validation.note.size}")
    private String note;
}
