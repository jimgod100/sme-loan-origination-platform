package com.smeloan.platform.application.repository;

import com.smeloan.platform.application.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByUbn(String ubn);

    boolean existsByUbn(String ubn);
}
