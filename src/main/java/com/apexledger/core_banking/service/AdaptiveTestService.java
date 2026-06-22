package com.apexledger.core_banking.service;

import com.apexledger.core_banking.dto.QuestionDTO;
import com.apexledger.core_banking.dto.SubmitAnswerRequest;
import com.apexledger.core_banking.dto.SubmitAnswerResponse;
import com.apexledger.core_banking.entity.ActiveTestSession;
import com.apexledger.core_banking.entity.RecruitTestQuestion;
import com.apexledger.core_banking.repository.ActiveTestSessionRepository;
import com.apexledger.core_banking.repository.RecruitTestQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdaptiveTestService {

    private final ActiveTestSessionRepository sessionRepository;
    private final RecruitTestQuestionRepository questionRepository;

    @Transactional
    public Object startTest(Long candidateId, Long testId) {

        Optional<ActiveTestSession> existingSession = sessionRepository
                .findByCandidateIdAndTestIdAndStatus(candidateId, testId, "IN_PROGRESS");

        ActiveTestSession session;

        if (existingSession.isPresent()) {
            session = existingSession.get();
        } else {

            session = new ActiveTestSession();
            session.setCandidateId(candidateId);
            session.setTestId(testId);
            session.setCurrentLevel("M");
            session.setStatus("IN_PROGRESS");

            sessionRepository.save(session);
        }

        // 3. FETCH THE QUESTION
        return fetchNextQuestionWithFallback(session);
    }

    @Transactional
    public SubmitAnswerResponse submitAnswer(SubmitAnswerRequest request) {

        // 1. Retrieve the active session
        ActiveTestSession session = sessionRepository.findByCandidateIdAndTestIdAndStatus(
                request.getCandidateId(), request.getTestId(), "IN_PROGRESS"
        ).orElseThrow(() -> new RuntimeException("No active test session found!"));

        // 2. Retrieve the question they just answered to check the correct answer
        RecruitTestQuestion answeredQuestion = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Question not found!"));

        boolean isCorrect = answeredQuestion.getCorrectAnswer().equalsIgnoreCase(request.getSelectedOption());

        // 3. Update the Adaptive Trackers
        updateSessionState(session, isCorrect);

        // 4. Update the Excluded IDs list (append the question they just saw)
        String currentIds = session.getAskedQuestionIds();

        String newExcludedIds = (currentIds == null || currentIds.trim().isEmpty()) ?
                String.valueOf(request.getQuestionId()) :
                currentIds + "," + request.getQuestionId();

        session.setAskedQuestionIds(newExcludedIds);

        // 5. Check the Kill Switches
        SubmitAnswerResponse response = new SubmitAnswerResponse();
        if (session.getTotalQuestionsAsked() >= 20 || session.getExpertQuestionsPassed() >= 5) {
            session.setStatus("COMPLETED");
            sessionRepository.save(session);

            response.setTestCompleted(true);
            response.setCompletionMessage("Test finished successfully. Thank you!");
            return response;
        }

        // 6. Fetch the Next Question (with Fallback Logic)
        RecruitTestQuestion nextQuestionEntity = fetchNextQuestionWithFallback(session);

        // 7. Map to DTO and save session
        sessionRepository.save(session);

        response.setTestCompleted(false);
        response.setNextQuestion(mapToDTO(nextQuestionEntity));
        return response;
    }

    private RecruitTestQuestion fetchNextQuestionWithFallback(ActiveTestSession session) {
        List<Long> excludedIds = parseExcludedIds(session.getAskedQuestionIds());

        // Attempt 1: Fetch at the current adapted level
        Optional<RecruitTestQuestion> question = questionRepository.findNextAdaptiveQuestion(
                session.getTestId(), session.getCurrentLevel(), excludedIds);

        if (question.isPresent()) return question.get();

        // Attempt 2: FALLBACK (The pool is empty!)
        // If HARD is empty, drop to MEDIUM. If EASY/MEDIUM is empty, go to the other.
        String fallbackLevel = session.getCurrentLevel().equals("H") ? "M" :
                (session.getCurrentLevel().equals("M") ? "E" : "M");

        return questionRepository.findNextAdaptiveQuestion(session.getTestId(), fallbackLevel, excludedIds)
                .orElseThrow(() -> new RuntimeException("Critical Error: Entire test pool is exhausted!"));
    }

    private void updateSessionState(ActiveTestSession session, boolean isCorrect) {
        session.setTotalQuestionsAsked(session.getTotalQuestionsAsked() + 1);

        if (isCorrect) {
            // Track expert questions for the ceiling kill switch
            if (session.getCurrentLevel().equals("H")) {
                session.setExpertQuestionsPassed(session.getExpertQuestionsPassed() + 1);
            }

            // Streak Logic: If they were negative, reset to +1. Otherwise add 1.
            int newStreak = session.getCurrentStreak() < 0 ? 1 : session.getCurrentStreak() + 1;
            session.setCurrentStreak(newStreak);

            // Level Up Check (+2)
            if (session.getCurrentStreak() >= 2) {
                if (session.getCurrentLevel().equals("E")) session.setCurrentLevel("M");
                else if (session.getCurrentLevel().equals("M")) session.setCurrentLevel("H");
                session.setCurrentStreak(0); // Reset streak after level change
            }
        } else {
            // Streak Logic: If they were positive, reset to -1. Otherwise subtract 1.
            int newStreak = session.getCurrentStreak() > 0 ? -1 : session.getCurrentStreak() - 1;
            session.setCurrentStreak(newStreak);

            // Level Down Check (-2)
            if (session.getCurrentStreak() <= -2) {
                if (session.getCurrentLevel().equals("H")) session.setCurrentLevel("M");
                else if (session.getCurrentLevel().equals("M")) session.setCurrentLevel("E");
                session.setCurrentStreak(0); // Reset streak after level change
            }
        }
    }

    private List<Long> parseExcludedIds(String commaSeparatedIds) {
        if (commaSeparatedIds == null || commaSeparatedIds.isEmpty()) {
            return List.of(-1L);
        }
        return Arrays.stream(commaSeparatedIds.split(","))
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    private QuestionDTO mapToDTO(RecruitTestQuestion entity) {
        QuestionDTO dto = new QuestionDTO();
        dto.setQuestionId(entity.getId());
        dto.setQuestionText(entity.getQuestion());
        dto.setOptionA(entity.getOptionA());
        dto.setOptionB(entity.getOptionB());
        dto.setOptionC(entity.getOptionC());
        dto.setOptionD(entity.getOptionD());
        dto.setDifficultyLevel(entity.getQuestionLevel());
        return dto;
    }
}
