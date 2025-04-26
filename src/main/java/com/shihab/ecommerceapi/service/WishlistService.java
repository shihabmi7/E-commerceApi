package com.shihab.ecommerceapi.service;

import com.shihab.ecommerceapi.model.Wishlist;
import com.shihab.ecommerceapi.repository.WishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WishlistService {
    
    @Autowired
    private WishlistRepository wishlistRepository;

    public List<Wishlist> findAll() {
        return wishlistRepository.findAll();
    }

    public Optional<Wishlist> findById(Integer id) {
        return wishlistRepository.findById(id);
    }

    public Wishlist save(Wishlist wishlist) {
        return wishlistRepository.save(wishlist);
    }

    public void deleteById(Integer id) {
        wishlistRepository.deleteById(id);
    }
}