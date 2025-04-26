package com.shihab.ecommerceapi.repository;

import com.shihab.ecommerceapi.model.Shipping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShippingRepository extends JpaRepository<Shipping, Integer> {
}