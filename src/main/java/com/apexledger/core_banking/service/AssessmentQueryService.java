package com.apexledger.core_banking.service;

import com.apexledger.core_banking.entity.RecruitTestQuestion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Service;

@Service
public class AssessmentQueryService {

    @PersistenceContext
    private EntityManager entityManager;


    public void saveQuestionProcedure(Long testId, RecruitTestQuestion question) {
        // Create the stored procedure query using the local procedure name
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("USP_SANKALP_QUESTION_ENTRY");

        // 1. Register Parameters (Matching the IN/OUT parameters of the procedure)
        query.registerStoredProcedureParameter("P_TEST_ID", Long.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_QUESTION_LEVEL", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_QUESTION", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_OPTION_A", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_OPTION_B", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_OPTION_C", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_OPTION_D", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_CORRECT_ANSWER", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_QUESTION_TYPE", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("P_MARKS", Double.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("O_STATUS_MESSAGE", String.class, ParameterMode.OUT);

        // 2. Assign Values from the Entity
        query.setParameter("P_TEST_ID", testId);
        query.setParameter("P_QUESTION_LEVEL", question.getQuestionLevel());
        query.setParameter("P_QUESTION", question.getQuestion());
        query.setParameter("P_OPTION_A", question.getOptionA());
        query.setParameter("P_OPTION_B", question.getOptionB());
        query.setParameter("P_OPTION_C", question.getOptionC());
        query.setParameter("P_OPTION_D", question.getOptionD());
        query.setParameter("P_CORRECT_ANSWER", question.getCorrectAnswer()); // Handled as null for subjective
        query.setParameter("P_QUESTION_TYPE", question.getQuestionType());
        query.setParameter("P_MARKS", question.getMarks());

        // 3. Execute and Parse Response
        try {
            query.execute();

            // Retrieve the OUT parameter status message
            String statusMessage = (String) query.getOutputParameterValue("O_STATUS_MESSAGE");

            // Evaluate database-side business validation errors
            if (statusMessage != null && statusMessage.startsWith("1-")) {
                throw new RuntimeException("Procedure validation failed: " + statusMessage.substring(2));
            }

        } catch (Exception e) {
            // Rethrowing an explicit exception ensures @Transactional flags trigger a complete rollback
            throw new RuntimeException("Database execution failed for question. Details: " + e.getMessage(), e);
        }
    }
}