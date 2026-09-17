package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.dto.CustomResponse;
import com.shihab.ecommerceapi.exception.EntityNotFoundException;
import com.shihab.ecommerceapi.model.Address;
import com.shihab.ecommerceapi.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
public class AddressController extends BaseController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    public ResponseEntity<CustomResponse<List<Address>>> getAll() {
        List<Address> addresses = addressService.findAll();
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Addresses fetched successfully", addresses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<Address>> getById(@PathVariable Integer id) {
        Address address = addressService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No address found with ID: " + id));
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Address fetched successfully", address));
    }

    @PostMapping
    public ResponseEntity<CustomResponse<Address>> create(@Valid @RequestBody Address address) {
        Address saved = addressService.save(address);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(new CustomResponse<>(HttpStatus.CREATED.value(), "Address created successfully", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomResponse<Address>> update(@PathVariable Integer id, @Valid @RequestBody Address updatedAddress) {
        updatedAddress.setId(id);
        Address saved = addressService.save(updatedAddress);
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Address updated successfully", saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        addressService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
