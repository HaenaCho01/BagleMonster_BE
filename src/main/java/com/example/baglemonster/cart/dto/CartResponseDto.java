package com.example.baglemonster.cart.dto;

import com.example.baglemonster.cart.entity.Cart;
import com.example.baglemonster.cartProduct.dto.CartProductResponseDto;
import com.example.baglemonster.cartProduct.entity.CartProduct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartResponseDto {
    private Long cartId;
    private Long storeId;
    private String storeName;
    private String phone;
    private String address;
    private List<CartProductResponseDto> products;
    private Integer totalPrice;
    private String storeStatus;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;

    public static CartResponseDto of(Cart cart) {
        List<CartProduct> cartProducts = cart.getCartProducts();
        return CartResponseDto.builder()
                .cartId(cart.getId())
                .storeId(cart.getStore().getId())
                .storeName(cart.getStore().getName())
                .phone(cart.getStore().getPhone())
                .address(cart.getStore().getAddress())
                .products(cartProducts.stream().map(CartProductResponseDto::of).toList())
                .totalPrice(cart.getTotalPrice())
                .storeStatus(cart.getStoreStatus().getStatus())
                .createdDate(cart.getCreatedDate())
                .modifiedDate(cart.getModifiedDate())
                .build();
    }
}
