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

    @Column(name = "DESIGNATION_ID")
    private Long designationId;

    @Column(name = "TOTAL_APPEARED_QUESTIONS")
    private Integer totalAppearedQuestions;

    @Column(name = "UPLOADED_BY", length = 100)
    private String uploadedBy;

    @Column(name = "UPLOADED_DATE")
    private LocalDateTime uploadedDate;

    @Column(name = "TEST_NAME", nullable = false, length = 200)
    private String testName;

    @Column(name = "TEST_PASSING_PERCENTAGE")
    private Double testPassingPercentage;

    @Column(name = "TEST_TYPE", length = 50)
    private String testType;

    @Column(name = "TOTAL_SUBJECTIVE_QUESTIONS")
    private Integer totalSubjectiveQuestions;

    @Column(name = "TOTAL_OBJECTIVE_QUESTIONS")
    private Integer totalObjectiveQuestions;

    @Column(name = "OBJECTIVE_PASSING_PERCENTAGE")
    private Double objectivePassingPercentage;

    @Column(name = "SUBJECTIVE_PASSING_PERCENTAGE")
    private Double subjectivePassingPercentage;

//    @Transient
//    private String sourceFileName;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RecruitTestQuestion> questions = new ArrayList<>();

    public void addQuestions(RecruitTestQuestion question) {
        questions.add(question);
        question.setTest(this);
    }
}
