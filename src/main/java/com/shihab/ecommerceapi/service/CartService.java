package com.shihab.ecommerceapi.service;

import com.shihab.ecommerceapi.model.Cart;
import com.shihab.ecommerceapi.model.Product;
import com.shihab.ecommerceapi.model.User;
import com.shihab.ecommerceapi.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CartService {
    
    @Autowired
    private CartRepository cartRepository;

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
            // 2b) otherwise create a new line item
            Cart cart = new Cart();
            User aUser = new User();
            aUser.setId(userId);
            cart.setUser(aUser);         // assumes User(int id) constructor

            Product aProduct = new Product();
            aProduct.setId(productId);
            cart.setProduct(aProduct); // assumes Product(int id) constructor
            cart.setQuantity(quantity);
            return cartRepository.save(cart);
        }
    }

}