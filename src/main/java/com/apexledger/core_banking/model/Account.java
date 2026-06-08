package com.apexledger.core_banking.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "sankalp_account")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @Column(unique = true, nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String currency;        // 'INR' , 'USD'

    @Column(nullable = false)
    private String status;          //'Active' , 'Frozen'

    @ManyToOne
    @JoinColumn(name = "user_uuid", nullable = false)
    private AppUser user;
}
