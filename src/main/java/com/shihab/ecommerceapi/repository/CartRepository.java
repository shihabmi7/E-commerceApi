package com.shihab.ecommerceapi.repository;

import com.shihab.ecommerceapi.model.Cart;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {

    // user/product are lazy on Cart; these queries fetch them eagerly in the same
    // SQL query (single JOIN) so callers can safely read them after the repository
    // call returns, without triggering a separate N+1 query or a
    // LazyInitializationException (spring.jpa.open-in-view=false closes the session
    // as soon as the repository method returns).
    @EntityGraph(attributePaths = {"user", "product"})
    @Override
    List<Cart> findAll();

    @EntityGraph(attributePaths = {"user", "product"})
    @Override
    Optional<Cart> findById(Integer id);

    @EntityGraph(attributePaths = {"user", "product"})
    List<Cart> findByUserId(Integer userId);

    @EntityGraph(attributePaths = {"user", "product"})
    Optional<Cart> findByUserIdAndProductId(Integer userId, Integer productId);
}