package com.apexledger.core_banking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "Sankalp_MediaMaster")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class MediaMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "media_master_seq_gen")
    @SequenceGenerator(name = "media_master_seq_gen", sequenceName = "MEDIA_MASTER_SEQ", allocationSize = 1)
    @Column(name = "MEDIA_ID")
    private Long id;

    @Column(name = "FILE_NAME", nullable = false, length = 255)
    private String fileName;

    @Column(name = "CONTENT_TYPE", nullable = false, length = 100)
    private String contentType;

    @Column(name = "FILE_SIZE", nullable = false)
    private Long fileSize;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "FILE_DATA", nullable = false)
    private byte[] fileData;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;
}
