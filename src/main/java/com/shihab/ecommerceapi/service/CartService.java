package com.shihab.ecommerceapi.service;

import com.shihab.ecommerceapi.exception.EntityNotFoundException;
import com.shihab.ecommerceapi.model.Cart;
import com.shihab.ecommerceapi.model.Product;
import com.shihab.ecommerceapi.model.User;
import com.shihab.ecommerceapi.repository.CartRepository;
import com.shihab.ecommerceapi.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    public List<Cart> findAll() {
        return cartRepository.findAll();
    }

    public Optional<Cart> findById(Integer id) {
        return cartRepository.findById(id);
    }

    public Cart save(Cart cart) {
        return cartRepository.save(cart);
    }

    public void deleteById(Integer id) {
        cartRepository.deleteById(id);
    }

    public List<Cart> findByUserId(Integer userId) {
        return cartRepository.findByUserId(userId);
    }

    public Cart addToCart(Integer userId, Integer productId, Integer quantity) {
        // 1) try to find an existing cart line
        Optional<Cart> existing = cartRepository
                .findByUserIdAndProductId(userId, productId);

        if (existing.isPresent()) {
            // 2a) if found, bump its quantity
            Cart cart = existing.get();
            cart.setQuantity(cart.getQuantity() + quantity);
            return cartRepository.save(cart);
        } else {
            // 2b) otherwise create a new line item, snapshotting the product's
            // current price so later price changes don't silently affect this cart line
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("No product found with ID: " + productId));

            Cart cart = new Cart();
            User aUser = new User();
            aUser.setId(userId);
            cart.setUser(aUser);         // assumes User(int id) constructor

            cart.setProduct(product);
            cart.setQuantity(quantity);
            cart.setPrice(product.getPrice());
            return cartRepository.save(cart);
        }
    }

}