package com.apexledger.core_banking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Sankalp_test_questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruitTestQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "recruit_question_seq_gen")
    @SequenceGenerator(name = "recruit_question_seq_gen", sequenceName = "RECRUIT_TEST_QUESTION_SEQ", allocationSize = 1)
    @Column(name = "QUESTION_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TEST_ID", nullable = false)
    @JsonIgnore
    private RecruitTest test;

    // --- Excel Data Columns ---
    @Column(name = "QUESTION_LEVEL")
    private String questionLevel;

    @Column(name = "QUESTION", length = 4000)
    private String question;

    @Column(name = "OPTION_A", length = 4000 )
    private String optionA;

    @Column(name = "OPTION_B", length = 4000)
    private String optionB;

    @Column(name = "OPTION_C", length = 4000)
    private String optionC;

    @Column(name = "OPTION_D", length = 4000)
    private String optionD;

    @Column(name = "CORRECT_ANSWER", nullable = false, length = 10)
    private String correctAnswer;

    @Column(name = "QUESTION_TYPE", nullable = false, length = 20)
    private String questionType;

    @Column(name = "MARKS", nullable = false)
    private Double marks;
}
