package com.smeloan.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the SME Loan Origination Platform Spring Boot application.
 *
 * <p>Technology stack:
 * <ul>
 *   <li>Java 17</li>
 *   <li>Spring Boot 3.2</li>
 *   <li>Spring Data JPA + Hibernate (PostgreSQL)</li>
 *   <li>Spring Security (JWT — TODO: wire up)</li>
 *   <li>Lombok</li>
 *   <li>MapStruct</li>
 * </ul>
 *
 * <p>Modules:
 * <ul>
 *   <li>{@code common} — shared entities, exceptions, DTOs</li>
 *   <li>{@code auth} — user authentication and authorisation</li>
 *   <li>{@code application} — loan application lifecycle management</li>
 *   <li>{@code onboarding} — customer self-service onboarding flow</li>
 *   <li>{@code externalcheck} — JCIC / AML / ETC check integration</li>
 *   <li>{@code scoring} — credit scoring engine</li>
 *   <li>{@code disbursement} — loan offer and repayment schedule generation</li>
 *   <li>{@code notification} — email and SMS notifications</li>
 * </ul>
 */
@SpringBootApplication
public class SmeloanApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmeloanApplication.class, args);
    }
}
