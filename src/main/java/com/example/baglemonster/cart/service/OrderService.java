package com.example.baglemonster.cart.service;

import com.example.baglemonster.cart.dto.OrdersResponseDto;
import com.example.baglemonster.cart.entity.Cart;
import com.example.baglemonster.cart.entity.StoreStatusEnum;
import com.example.baglemonster.cart.event.OrderCancelEventPublisher;
import com.example.baglemonster.cart.repository.CartRepository;
import com.example.baglemonster.cartProduct.entity.CartProduct;
import com.example.baglemonster.common.exception.NotFoundException;
import com.example.baglemonster.common.exception.UnauthorizedException;
import com.example.baglemonster.notification.entity.Notification;
import com.example.baglemonster.notification.repository.NotificationRepository;
import com.example.baglemonster.store.entity.Store;
import com.example.baglemonster.store.repository.StoreRepository;
import com.example.baglemonster.user.entity.User;
import com.example.baglemonster.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartRepository cartRepository;
    private final StoreRepository storeRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final OrderCancelEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public OrdersResponseDto selectOrders(User user, Long storeId) {
        confirmStore(user, storeId);

        return OrdersResponseDto.of(getOrders(storeId));
    }

    @Transactional
    public void readOrder(User user, Long storeId, Long orderId) {
        confirmStore(user, storeId);

        Cart order = getOrder(orderId);
        if (order.getStoreStatus().equals(StoreStatusEnum.NEWORDER)) {
            order.editStoreStatus(StoreStatusEnum.READ);
            Notification notification = findNotification(order);
            notification.readNotification();
        }
    }

    @Transactional
    public void completeOrder(User user, Long storeId, Long orderId) {
        confirmStore(user, storeId);
        Cart order = getOrder(orderId);
        order.editStoreStatus(StoreStatusEnum.SOLD);

        List<CartProduct> cartProducts = order.getCartProducts();
        for (CartProduct cartProduct : cartProducts) {
            cartProduct.getProduct().addPopularity(cartProduct.getQuantity());
        }
    }

    @Transactional
    public void cancelOrder(User user, Long storeId, Long orderId) {
        confirmStore(user, storeId);
        Cart order = getOrder(orderId);
        order.editStoreStatus(StoreStatusEnum.CANCELED);

        // 이벤트 발생 -> 알림 생성
        User customer = findUser(order.getUser().getId());
        eventPublisher.publishCartOrderEvent(order, customer);
    }

    private void confirmStore(User user, Long storeId) {
        Store store = storeRepository.findById(storeId).orElseThrow(() ->
                new NotFoundException("선택한 가게는 존재하지 않습니다."));

        if (!store.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("해당 가게에 대한 권한이 없습니다.");
        }
    }

    private List<Cart> getOrders(Long storeId) {
        return cartRepository.findAllByStoreIdAndStatus(storeId, true);
    }

    private Cart getOrder(Long orderId) {
        return cartRepository.findById(orderId).orElseThrow(() ->
                new NotFoundException("선택한 주문은 존재하지 않습니다."));
    }

    // id로 알림 찾기
    private Notification findNotification(Cart order) {
        return notificationRepository.findByCart(order).orElseThrow(() ->
                new IllegalArgumentException("선택한 알림은 존재하지 않습니다.")
        );
    }

    // ID로 유저 찾기
    private User findUser(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }
}
