package com.apexledger.core_banking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ACTIVE_TEST_SESSIONS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActiveTestSession {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "session_seq_gen")
    @SequenceGenerator(name = "session_seq_gen", sequenceName = "SEQ_TEST_SESSION_ID", allocationSize = 1)
    @Column(name = "SESSION_ID")
    private Long sessionId;

    @Column(name = "CANDIDATE_ID", nullable = false)
    private Long candidateId;

    @Column(name = "TEST_ID", nullable = false)
    private Long testId;

    @Column(name = "CURRENT_LEVEL", nullable = false)
    private String currentLevel = "MEDIUM";

    @Column(name = "CURRENT_STREAK", nullable = false)
    private int currentStreak = 0; // Tracks the +2 or -2 logic

    @Column(name = "TOTAL_QUESTIONS_ASKED", nullable = false)
    private int totalQuestionsAsked = 0; // Tracks our Max Cap of 20

    @Column(name = "EXPERT_QUESTIONS_PASSED", nullable = false)
    private int expertQuestionsPassed = 0; // Tracks our Ceiling of 5

    @Lob
    @Column(name = "ASKED_QUESTION_IDS")
    private String askedQuestionIds = ""; // Stores IDs like: "101,154,203"

    @Column(name = "STATUS", nullable = false)
    private String status = "IN_PROGRESS"; // Flips to COMPLETED when test ends
}