package com.shihab.ecommerceapi.service;

import com.shihab.ecommerceapi.exception.EntityNotFoundException;
import com.shihab.ecommerceapi.model.Cart;
import com.shihab.ecommerceapi.model.Product;
import com.shihab.ecommerceapi.model.User;
import com.shihab.ecommerceapi.repository.CartRepository;
import com.shihab.ecommerceapi.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
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

    // Deliberately NOT @Transactional: each repository call below must commit (or fail)
    // in its own transaction so the catch block's fallback lookup can run after a failed
    // insert. Wrapping this method in one transaction would abort the whole transaction
    // on the constraint violation (Postgres poisons the transaction on any SQL error),
    // making the retry query below fail too.
    public Cart addToCart(Integer userId, Integer productId, Integer quantity) {
        // 1) try to find an existing cart line
        Optional<Cart> existing = cartRepository
                .findByUserIdAndProductId(userId, productId);

        if (existing.isPresent()) {
            // 2a) if found, bump its quantity
            return bumpQuantity(existing.get(), quantity);
        }

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

        try {
            // saveAndFlush (not save) forces the unique-constraint check to run right here,
            // inside this method's own repository-managed transaction, instead of being
            // deferred until some later, unrelated flush/commit.
            return cartRepository.saveAndFlush(cart);
        } catch (DataIntegrityViolationException raceLost) {
            // Two concurrent requests can both pass the "does it exist?" check above with
            // "no" (classic check-then-act race — e.g. a user double-clicking "Add to Cart").
            // The DB's unique(user_id, product_id) constraint catches the duplicate insert;
            // fall back to updating the row the other request just created instead of
            // leaving two separate lines for the same product.
            Cart concurrentlyInserted = cartRepository.findByUserIdAndProductId(userId, productId)
                    .orElseThrow(() -> raceLost);
            return bumpQuantity(concurrentlyInserted, quantity);
        }
    }

    private Cart bumpQuantity(Cart cart, Integer quantity) {
        cart.setQuantity(cart.getQuantity() + quantity);
        return cartRepository.save(cart);
    }

}