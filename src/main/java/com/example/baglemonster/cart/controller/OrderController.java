package com.example.baglemonster.cart.controller;

import com.example.baglemonster.cart.dto.OrderResponseDto;
import com.example.baglemonster.cart.dto.OrdersResponseDto;
import com.example.baglemonster.cart.service.OrderService;
import com.example.baglemonster.common.dto.ApiResponseDto;
import com.example.baglemonster.common.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "가게 주문 API", description = "가게의 전체/단일 주문 확인, 취소, 판매 완료와 관련된 API 정보를 담고 있습니다.")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "전체 주문 조회", description = "가게의 전체 주문내역을 조회합니다.")
    @GetMapping("/stores/{storeId}/orders")
    public ResponseEntity<OrdersResponseDto> selectOrders(@AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable Long storeId) {
         OrdersResponseDto result = orderService.selectOrders(userDetails.getUser(), storeId);
         return ResponseEntity.ok(result);
    }

    @Operation(summary = "주문 확인", description = "주문의 상태를 읽음으로 변경합니다.")
    @PutMapping("/stores/{storeId}/orders/{orderId}/read")
    public ResponseEntity<ApiResponseDto> selectOrder(@AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable Long storeId, @PathVariable Long orderId) {
        orderService.readOrder(userDetails.getUser(), storeId, orderId);
        return ResponseEntity.ok().body(new ApiResponseDto(HttpStatus.OK.value(), "해당 주문이 읽음 처리되었습니다."));
    }

    @Operation(summary = "판매 완료", description = "판매가 완료된 주문의 상태를 판매됨으로 변경합니다.")
    @PutMapping("/stores/{storeId}/orders/{orderId}/sold")
    public ResponseEntity<ApiResponseDto> completeOrder(@AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable Long storeId, @PathVariable Long orderId) {
        orderService.completeOrder(userDetails.getUser(), storeId, orderId);
        return ResponseEntity.ok().body(new ApiResponseDto(HttpStatus.OK.value(), "해당 주문이 판매되었습니다."));
    }

    @Operation(summary = "주문 취소", description = "재고 소진 등의 이유로 주문의 상태를 취소됨으로 변경합니다.")
    @PutMapping("/stores/{storeId}/orders/{orderId}/canceled")
    public ResponseEntity<ApiResponseDto> cancelOrder(@AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable Long storeId, @PathVariable Long orderId) {
        orderService.cancelOrder(userDetails.getUser(), storeId, orderId);
        return ResponseEntity.ok().body(new ApiResponseDto(HttpStatus.NO_CONTENT.value(), "해당 주문이 취소되었습니다."));
    }
}