package com.shihab.ecommerceapi.service;

import com.shihab.ecommerceapi.model.Cart;
import com.shihab.ecommerceapi.model.Product;
import com.shihab.ecommerceapi.model.User;
import com.shihab.ecommerceapi.repository.CartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void findAll_returnsList() {
        when(cartRepository.findAll()).thenReturn(List.of(new Cart()));
        assertThat(cartService.findAll()).hasSize(1);
    }

    @Test
    void findById_returnsCart_whenPresent() {
        Cart cart = new Cart(1, new User(), new Product(), 2);
        when(cartRepository.findById(1)).thenReturn(Optional.of(cart));
        assertThat(cartService.findById(1)).isPresent();
    }

    @Test
    void save_delegatesToRepository() {
        Cart cart = new Cart(null, new User(), new Product(), 1);
        when(cartRepository.save(cart)).thenReturn(cart);
        assertThat(cartService.save(cart)).isEqualTo(cart);
    }

    @Test
    void deleteById_delegatesToRepository() {
        doNothing().when(cartRepository).deleteById(1);
        cartService.deleteById(1);
        verify(cartRepository).deleteById(1);
    }

    @Test
    void findByUserId_returnsUserCart() {
        Cart c = new Cart(1, new User(), new Product(), 3);
        when(cartRepository.findByUserId(5)).thenReturn(List.of(c));
        assertThat(cartService.findByUserId(5)).hasSize(1);
    }

    @Test
    void addToCart_updatesQuantity_whenItemAlreadyExists() {
        Cart existing = new Cart(1, new User(), new Product(), 2);
        when(cartRepository.findByUserIdAndProductId(1, 10)).thenReturn(Optional.of(existing));
        when(cartRepository.save(existing)).thenReturn(existing);

        Cart result = cartService.addToCart(1, 10, 3);

        assertThat(result.getQuantity()).isEqualTo(5); // 2 + 3
        verify(cartRepository).save(existing);
    }

    @Test
    void addToCart_createsNewItem_whenItemDoesNotExist() {
        when(cartRepository.findByUserIdAndProductId(1, 10)).thenReturn(Optional.empty());
        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        Cart saved = new Cart(2, new User(), new Product(), 3);
        when(cartRepository.save(any(Cart.class))).thenReturn(saved);

        Cart result = cartService.addToCart(1, 10, 3);

        verify(cartRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantity()).isEqualTo(3);
        assertThat(result.getId()).isEqualTo(2);
    }
}
