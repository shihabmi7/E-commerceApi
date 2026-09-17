package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.dto.AddToCartRequest;
import com.shihab.ecommerceapi.dto.CustomResponse;
import com.shihab.ecommerceapi.dto.ProductInCartDto;
import com.shihab.ecommerceapi.exception.EntityNotFoundException;
import com.shihab.ecommerceapi.model.Cart;
import com.shihab.ecommerceapi.model.Product;
import com.shihab.ecommerceapi.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<Cart>> getById(@PathVariable Integer id) {
        Cart cart = cartService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No cart entry found with ID: " + id));
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Cart entry fetched successfully", cart));
    }

    @PostMapping
    public ResponseEntity<CustomResponse<Cart>> create(@Valid @RequestBody Cart cart) {
        Cart saved = cartService.save(cart);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(new CustomResponse<>(HttpStatus.CREATED.value(), "Cart entry created successfully", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomResponse<Cart>> update(@PathVariable Integer id, @Valid @RequestBody Cart updatedCart) {
        updatedCart.setId(id);
        Cart saved = cartService.save(updatedCart);
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Cart entry updated successfully", saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        cartService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private static final double PRICE_CHANGE_EPSILON = 0.0001;

    /**
     * Returns a list of products in the user's cart, each with its aggregated quantity
     * and a flag/delta showing whether the product's live price has moved since it
     * was added (the customer is still charged the locked-in price at checkout).
     */
    @GetMapping
    public ResponseEntity<CustomResponse<List<ProductInCartDto>>> getCart(@RequestParam Integer userId) {
        // 1) fetch all Cart entries for the user
        List<Cart> carts = cartService.findByUserId(userId);

        // 2) group by product, preserving each line's locked-in price
        Map<Product, List<Cart>> byProduct = carts.stream()
                .collect(Collectors.groupingBy(Cart::getProduct, LinkedHashMap::new, Collectors.toList()));

        // 3) map to DTOs
        List<ProductInCartDto> result = byProduct.entrySet().stream()
                .map(entry -> {
                    Product p = entry.getKey();
                    List<Cart> lines = entry.getValue();

                    int totalQuantity = lines.stream().mapToInt(Cart::getQuantity).sum();
                    // locked-in price as of when the line was first added; falls back to the
                    // live price for legacy/raw-created rows that were never snapshotted
                    Double lockedPrice = lines.get(0).getPrice() != null ? lines.get(0).getPrice() : p.getPrice();
                    Double currentPrice = p.getPrice();
                    boolean priceChanged = Math.abs(currentPrice - lockedPrice) > PRICE_CHANGE_EPSILON;
                    double priceDelta = currentPrice - lockedPrice;

                    return new ProductInCartDto(
                            p.getId(),
                            p.getName(),
                            p.getDescription(),
                            currentPrice,
                            p.getStock(),
                            p.getCategory(),
                            totalQuantity,
                            lockedPrice,
                            priceChanged,
                            priceDelta
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Cart fetched successfully", result));
    }

    @PostMapping("/add")
    public ResponseEntity<CustomResponse<Cart>> addToCart(@Valid @RequestBody AddToCartRequest req) {
        Cart cart = cartService.addToCart(
                req.getUserId(),
                req.getProductId(),
                req.getQuantity()
        );
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Item added to cart successfully", cart));
    }



}
