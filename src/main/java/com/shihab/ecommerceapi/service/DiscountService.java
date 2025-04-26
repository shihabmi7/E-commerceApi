package com.shihab.ecommerceapi.service;

import com.shihab.ecommerceapi.model.Discount;
import com.shihab.ecommerceapi.repository.DiscountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DiscountService {
    
    @Autowired
    private DiscountRepository discountRepository;

    public List<Discount> findAll() {
        return discountRepository.findAll();
    }

    public Optional<Discount> findById(Integer id) {
        return discountRepository.findById(id);
    }

    public Discount save(Discount discount) {
        return discountRepository.save(discount);
    }

    public void deleteById(Integer id) {
        discountRepository.deleteById(id);
    }
}