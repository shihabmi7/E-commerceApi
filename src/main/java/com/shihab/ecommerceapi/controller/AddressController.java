package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.model.Address;
import com.shihab.ecommerceapi.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @GetMapping
    public List<Address> getAll() {
        return addressService.findAll();
    }

    @GetMapping("/{id}")
    public Optional<Address> getById(@PathVariable Integer id) {
        return addressService.findById(id);
    }

    @PostMapping
    public Address create(@RequestBody Address address) {
        return addressService.save(address);
    }

    @PutMapping("/{id}")
    public Address update(@PathVariable Integer id, @RequestBody Address updatedAddress) {
        updatedAddress.setId(id);
        return addressService.save(updatedAddress);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        addressService.deleteById(id);
    }
}

