package com.shihab.ecommerceapi.service;

import com.shihab.ecommerceapi.model.Address;
import com.shihab.ecommerceapi.repository.AddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AddressService {
    
    @Autowired
    private AddressRepository addressRepository;

    public List<Address> findAll() {
        return addressRepository.findAll();
    }

    public Optional<Address> findById(Integer id) {
        return addressRepository.findById(id);
    }

    public Address save(Address address) {
        return addressRepository.save(address);
    }

    public void deleteById(Integer id) {
        addressRepository.deleteById(id);
    }
}