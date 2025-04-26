package com.shihab.ecommerceapi.service;

import com.shihab.ecommerceapi.model.Shipping;
import com.shihab.ecommerceapi.repository.ShippingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ShippingService {
    
    @Autowired
    private ShippingRepository shippingRepository;

    public List<Shipping> findAll() {
        return shippingRepository.findAll();
    }

    public Optional<Shipping> findById(Integer id) {
        return shippingRepository.findById(id);
    }

    public Shipping save(Shipping shipping) {
        return shippingRepository.save(shipping);
    }

    public void deleteById(Integer id) {
        shippingRepository.deleteById(id);
    }
}