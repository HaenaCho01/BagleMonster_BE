package com.example.baglemonster.cart.service;

import com.example.baglemonster.cart.dto.CartRequestDto;
import com.example.baglemonster.cart.dto.CartResponseDto;
import com.example.baglemonster.cart.dto.CartsResponseDto;
import com.example.baglemonster.cart.dto.OrderRequestDto;
import com.example.baglemonster.cart.entity.Cart;
import com.example.baglemonster.cart.repository.CartRepository;
import com.example.baglemonster.cartProduct.dto.CartProductResponseDto;
import com.example.baglemonster.cartProduct.entity.CartProduct;
import com.example.baglemonster.cartProduct.repository.CartProductRepository;
import com.example.baglemonster.common.exception.NotFoundException;
import com.example.baglemonster.common.exception.UnauthorizedException;
import com.example.baglemonster.product.entity.Product;
import com.example.baglemonster.product.service.ProductService;
import com.example.baglemonster.store.entity.Store;
import com.example.baglemonster.store.service.StoreService;
import com.example.baglemonster.user.entity.User;
import com.example.baglemonster.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final CartProductRepository cartProductRepository;
    private final StoreService storeService;
    private final ProductService productService;
    private final UserService userService;

    // 장바구니 메뉴 추가
    @Transactional
    public void createCart(CartRequestDto cartRequestDto, User user) {
        // 장바구니 확인 후 가져오기
        User consumer = userService.findUser(user.getId());
        Cart cart = getCart(consumer, cartRequestDto);

        // 장바구니에 상품 담기 -> 이미 해당 상품이 담겨 있는 경우 예외 처리
        Product product = productService.findProduct(cartRequestDto.getProductId());
        CartProduct cartProduct = findCartProduct(cart, product);
        if (cartProduct != null) {
            throw new IllegalArgumentException("해당 상품은 이미 장바구니에 담겨있습니다.");
        } else {
            cartProduct = cartRequestDto.toEntityCartProduct(cart, product);
            cartProductRepository.save(cartProduct);
        }

        // 장바구니 총액 수정하기
        Integer totalPrice = cart.getTotalPrice() + (product.getPrice() * cartRequestDto.getQuantity());
        cart.editTotalPrice(totalPrice);

        // 장바구니 저장하기
        cartRepository.save(cart);
    }

    // 장바구니 조회
    @Transactional(readOnly = true)
    public CartResponseDto selectCart(User user) {
        User consumer = userService.findUser(user.getId());
        Cart cart = findCartByUserAndStatus(consumer);
        return CartResponseDto.of(cart);
    }

    // 특정 주문 수량 감소
    @Transactional
    public CartProductResponseDto subtractCartProduct(Long cartId, Long productId, User user) {
        CartProduct cartProduct = getCartProduct(cartId, productId, user);
        cartProduct.subtractCartProductQuantity();
        if (cartProduct.getQuantity() == 0) {
            deleteCartProduct(cartId, productId, user);
        }
        return CartProductResponseDto.of(cartProduct);
    }

    // 특정 주문 수량 추가
    @Transactional
    public CartProductResponseDto addCartProduct(Long cartId, Long productId, User user) {
        CartProduct cartProduct = getCartProduct(cartId, productId, user);
        cartProduct.addCartProductQuantity();
        return CartProductResponseDto.of(cartProduct);
    }

    // 특정 주문 취소
    @Transactional
    public void deleteCartProduct(Long cartId, Long productId, User user) {
        CartProduct cartProduct = getCartProduct(cartId, productId, user);
        cartProductRepository.delete(cartProduct);
    }

    // 전체 주문 취소
    @Transactional
    public void deleteCart(Long cartId, User user) {
        Cart cart = getCart(cartId, user);
        cartRepository.delete(cart);
    }

    // 장바구니 주문 완료
    @Transactional
    public void orderCart(Long cartId, OrderRequestDto orderRequestDto, User user) {
        Cart cart = getCart(cartId, user);
        cart.order(orderRequestDto);
    }

    // 주문 내역 조회
    @Transactional(readOnly = true)
    public CartsResponseDto selectCarts(User user) {
        User consumer = userService.findUser(user.getId());
        List<Cart> carts = cartRepository.findAllByUser(consumer).stream().toList();
        return CartsResponseDto.of(carts);
    }

    // ------------private 메소드--------------

    // 장바구니 추가 시 확인 후 가져오기
    private Cart getCart(User user, CartRequestDto cartRequestDto) {
        // 장바구니가 이미 존재할 경우
        if (findCartByUserAndStatus(user) != null) {
            Cart cart = findCartByUserAndStatus(user);
            // 다른 가게의 장바구니가 담겨있을 경우 -> 에외처리
            if (!cart.getStore().getId().equals(cartRequestDto.getStoreId())) {
                throw new IllegalArgumentException("다른 가게의 상품이 이미 장바구니에 담겨있습니다.");
            } else {
                // 기존 가게일 경우 기존 장바구니 가져오기
                return cart;
            }
        } else {
            // 장바구니가 존재하지 않을 경우 새로 만들기
            Store store = storeService.findStore(cartRequestDto.getStoreId());
            return cartRequestDto.toCart(user, store);
        }
    }

    // 장바구니 추가 외 동작 확인 후 가져오기
    private Cart getCart(Long cartId, User user) {
        Cart cart = findCart(cartId);
        if (!cart.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("해당 장바구니에 대한 권한이 없습니다.");
        }
        return cart;
    }

    // 유저로 장바구니 주문 안된 것 있는지 확인 후 가져오기
    private Cart findCartByUserAndStatus(User user) {
        boolean status = false;
        return cartRepository.findByUserAndStatus(user, status);
    }

    // ID로 장바구니 찾기
    private Cart findCart(Long cartId) {
        return cartRepository.findById(cartId).orElseThrow(() ->
                new NotFoundException("선택한 장바구니는 존재하지 않습니다.")
        );
    }

    // 장바구니 상품 찾아 가져오기
    private CartProduct getCartProduct(Long cartId, Long productId, User user) {
        Cart cart = getCart(cartId, user);
        Product product = productService.findProduct(productId);
        return cartProductRepository.findByCartAndProduct(cart, product).orElseThrow(() ->
                new NotFoundException("선택한 장바구니 상품은 존재하지 않습니다.")
        );
    }

    // 장바구니 상품 찾기
    private CartProduct findCartProduct(Cart cart, Product product) {
        return cartProductRepository.findByCartAndProduct(cart, product).orElse(null);
    }
}
