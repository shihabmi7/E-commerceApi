package com.shihab.ecommerceapi.service;

import com.shihab.ecommerceapi.model.*;
import com.shihab.ecommerceapi.repository.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Covers the remaining simple CRUD services that each delegate directly to a JPA repository.
 */
@ExtendWith(MockitoExtension.class)
class CrudServicesTest {

    // ─── AddressService ────────────────────────────────────────────────────────

    @Nested
    class AddressServiceTests {
        @Mock AddressRepository repo;
        @InjectMocks AddressService service;

        @Test void findAll() { when(repo.findAll()).thenReturn(List.of(new Address())); assertThat(service.findAll()).hasSize(1); }
        @Test void findById_present() { when(repo.findById(1)).thenReturn(Optional.of(new Address())); assertThat(service.findById(1)).isPresent(); }
        @Test void findById_absent() { when(repo.findById(9)).thenReturn(Optional.empty()); assertThat(service.findById(9)).isEmpty(); }
        @Test void save() { Address a = new Address(); when(repo.save(a)).thenReturn(a); assertThat(service.save(a)).isEqualTo(a); }
        @Test void deleteById() { doNothing().when(repo).deleteById(1); service.deleteById(1); verify(repo).deleteById(1); }
    }

    // ─── DiscountService ───────────────────────────────────────────────────────

    @Nested
    class DiscountServiceTests {
        @Mock DiscountRepository repo;
        @InjectMocks DiscountService service;

        @Test void findAll() { when(repo.findAll()).thenReturn(List.of(new Discount())); assertThat(service.findAll()).hasSize(1); }
        @Test void findById_present() { when(repo.findById(1)).thenReturn(Optional.of(new Discount())); assertThat(service.findById(1)).isPresent(); }
        @Test void findById_absent() { when(repo.findById(9)).thenReturn(Optional.empty()); assertThat(service.findById(9)).isEmpty(); }
        @Test void save() { Discount d = new Discount(); when(repo.save(d)).thenReturn(d); assertThat(service.save(d)).isEqualTo(d); }
        @Test void deleteById() { doNothing().when(repo).deleteById(1); service.deleteById(1); verify(repo).deleteById(1); }
    }

    // ─── OrderItemService ──────────────────────────────────────────────────────

    @Nested
    class OrderItemServiceTests {
        @Mock OrderItemRepository repo;
        @InjectMocks OrderItemService service;

        @Test void findAll() { when(repo.findAll()).thenReturn(List.of(new OrderItem())); assertThat(service.findAll()).hasSize(1); }
        @Test void findById_present() { when(repo.findById(1)).thenReturn(Optional.of(new OrderItem())); assertThat(service.findById(1)).isPresent(); }
        @Test void findById_absent() { when(repo.findById(9)).thenReturn(Optional.empty()); assertThat(service.findById(9)).isEmpty(); }
        @Test void save() { OrderItem oi = new OrderItem(); when(repo.save(oi)).thenReturn(oi); assertThat(service.save(oi)).isEqualTo(oi); }
        @Test void deleteById() { doNothing().when(repo).deleteById(1); service.deleteById(1); verify(repo).deleteById(1); }
    }

    // ─── PaymentService ────────────────────────────────────────────────────────

    @Nested
    class PaymentServiceTests {
        @Mock PaymentRepository repo;
        @InjectMocks PaymentService service;

        @Test void findAll() { when(repo.findAll()).thenReturn(List.of(new Payment())); assertThat(service.findAll()).hasSize(1); }
        @Test void findById_present() { when(repo.findById(1)).thenReturn(Optional.of(new Payment())); assertThat(service.findById(1)).isPresent(); }
        @Test void findById_absent() { when(repo.findById(9)).thenReturn(Optional.empty()); assertThat(service.findById(9)).isEmpty(); }
        @Test void save() { Payment p = new Payment(); when(repo.save(p)).thenReturn(p); assertThat(service.save(p)).isEqualTo(p); }
        @Test void deleteById() { doNothing().when(repo).deleteById(1); service.deleteById(1); verify(repo).deleteById(1); }
    }

    // ─── ProductImageService ───────────────────────────────────────────────────

    @Nested
    class ProductImageServiceTests {
        @Mock ProductImageRepository repo;
        @InjectMocks ProductImageService service;

        @Test void findAll() { when(repo.findAll()).thenReturn(List.of(new ProductImage())); assertThat(service.findAll()).hasSize(1); }
        @Test void findById_present() { when(repo.findById(1)).thenReturn(Optional.of(new ProductImage())); assertThat(service.findById(1)).isPresent(); }
        @Test void findById_absent() { when(repo.findById(9)).thenReturn(Optional.empty()); assertThat(service.findById(9)).isEmpty(); }
        @Test void save() { ProductImage pi = new ProductImage(); when(repo.save(pi)).thenReturn(pi); assertThat(service.save(pi)).isEqualTo(pi); }
        @Test void deleteById() { doNothing().when(repo).deleteById(1); service.deleteById(1); verify(repo).deleteById(1); }
    }

    // ─── ReviewService ─────────────────────────────────────────────────────────

    @Nested
    class ReviewServiceTests {
        @Mock ReviewRepository repo;
        @InjectMocks ReviewService service;

        @Test void findAll() { when(repo.findAll()).thenReturn(List.of(new Review())); assertThat(service.findAll()).hasSize(1); }
        @Test void findById_present() { when(repo.findById(1)).thenReturn(Optional.of(new Review())); assertThat(service.findById(1)).isPresent(); }
        @Test void findById_absent() { when(repo.findById(9)).thenReturn(Optional.empty()); assertThat(service.findById(9)).isEmpty(); }
        @Test void save() { Review r = new Review(); when(repo.save(r)).thenReturn(r); assertThat(service.save(r)).isEqualTo(r); }
        @Test void deleteById() { doNothing().when(repo).deleteById(1); service.deleteById(1); verify(repo).deleteById(1); }
    }

    // ─── ShippingService ───────────────────────────────────────────────────────

    @Nested
    class ShippingServiceTests {
        @Mock ShippingRepository repo;
        @InjectMocks ShippingService service;

        @Test void findAll() { when(repo.findAll()).thenReturn(List.of(new Shipping())); assertThat(service.findAll()).hasSize(1); }
        @Test void findById_present() { when(repo.findById(1)).thenReturn(Optional.of(new Shipping())); assertThat(service.findById(1)).isPresent(); }
        @Test void findById_absent() { when(repo.findById(9)).thenReturn(Optional.empty()); assertThat(service.findById(9)).isEmpty(); }
        @Test void save() { Shipping s = new Shipping(); when(repo.save(s)).thenReturn(s); assertThat(service.save(s)).isEqualTo(s); }
        @Test void deleteById() { doNothing().when(repo).deleteById(1); service.deleteById(1); verify(repo).deleteById(1); }
    }

    // ─── WishlistService ───────────────────────────────────────────────────────

    @Nested
    class WishlistServiceTests {
        @Mock WishlistRepository repo;
        @InjectMocks WishlistService service;

        @Test void findAll() { when(repo.findAll()).thenReturn(List.of(new Wishlist())); assertThat(service.findAll()).hasSize(1); }
        @Test void findById_present() { when(repo.findById(1)).thenReturn(Optional.of(new Wishlist())); assertThat(service.findById(1)).isPresent(); }
        @Test void findById_absent() { when(repo.findById(9)).thenReturn(Optional.empty()); assertThat(service.findById(9)).isEmpty(); }
        @Test void save() { Wishlist w = new Wishlist(); when(repo.save(w)).thenReturn(w); assertThat(service.save(w)).isEqualTo(w); }
        @Test void deleteById() { doNothing().when(repo).deleteById(1); service.deleteById(1); verify(repo).deleteById(1); }
    }
}
