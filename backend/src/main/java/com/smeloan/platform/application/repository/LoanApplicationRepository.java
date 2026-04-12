package com.smeloan.platform.application.repository;

import com.smeloan.platform.application.entity.ApplicationStatus;
import com.smeloan.platform.application.entity.LoanApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    Optional<LoanApplication> findByApplicationNumber(String applicationNumber);

    Optional<LoanApplication> findByCustomerToken(String customerToken);

    Page<LoanApplication> findByStatus(ApplicationStatus status, Pageable pageable);

    Page<LoanApplication> findByRmUser_Id(Long rmUserId, Pageable pageable);

    long countByStatus(ApplicationStatus status);

    @Query("SELECT la FROM LoanApplication la " +
           "LEFT JOIN FETCH la.customer " +
           "LEFT JOIN FETCH la.rmUser " +
           "WHERE la.id = :id")
    Optional<LoanApplication> findByIdWithDetails(Long id);
}
