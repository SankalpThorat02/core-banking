package com.apexledger.core_banking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "Sankalp_tests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruitTest {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "recruit_test_seq_gen")
    @SequenceGenerator(name = "recruit_test_seq_gen", sequenceName = "RECRUIT_TEST_SEQ", allocationSize = 1)
    @Column(name = "TEST_ID")
    private Long id;

    @Column(name = "TEST_NAME", nullable = false, length = 200)
    private String testName;

    @Column(name = "SOURCE_FILE_NAME", length = 255)
    private String sourceFileName;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RecruitTestQuestion> questions = new ArrayList<>();

    public void addQuestions(RecruitTestQuestion question) {
        questions.add(question);
        question.setTest(this);
    }
}
