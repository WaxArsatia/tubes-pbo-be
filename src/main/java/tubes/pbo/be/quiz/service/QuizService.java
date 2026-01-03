package tubes.pbo.be.quiz.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tubes.pbo.be.quiz.dto.*;
import tubes.pbo.be.quiz.model.Question;
import tubes.pbo.be.quiz.model.Quiz;
import tubes.pbo.be.quiz.repository.QuestionRepository;
import tubes.pbo.be.quiz.repository.QuizRepository;
import tubes.pbo.be.shared.exception.AiServiceException;
import tubes.pbo.be.shared.exception.ForbiddenException;
import tubes.pbo.be.shared.exception.ResourceNotFoundException;
import tubes.pbo.be.shared.exception.ValidationException;
import tubes.pbo.be.summary.model.Summary;
import tubes.pbo.be.summary.repository.SummaryRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {
    
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final SummaryRepository summaryRepository;
    private final QuestionGenerationService questionGenerationService;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public QuizResponse generateQuiz(Long userId, QuizRequest request) {
        // Validate numberOfQuestions
        if (request.getNumberOfQuestions() != 5 && 
            request.getNumberOfQuestions() != 10 && 
            request.getNumberOfQuestions() != 15) {
            throw new ValidationException("Number of questions must be exactly 5, 10, or 15");
        }
        
        // Check summary exists and user owns it : sprint
        Summary summary = summaryRepository.findByIdAndUserId(request.getSummaryId(), userId)
                .orElseThrow(() -> new ForbiddenException("You do not have access to this summary"));
        
        // Parse difficulty (case-insensitive)
        Quiz.Difficulty difficulty;
        try {
            difficulty = Quiz.Difficulty.valueOf(request.getDifficulty().toUpperCase());
        } catch (IllegalArgumentException _) {
            throw new ValidationException("Invalid difficulty. Must be easy, medium, or hard");
        }
        
        // Create quiz entity
        Quiz quiz = new Quiz();
        quiz.setUserId(userId);
        quiz.setSummaryId(summary.getId());
        quiz.setDifficulty(difficulty);
        quiz.setNumberOfQuestions(request.getNumberOfQuestions());
        quiz.setIsSubmitted(false);
        quiz.setCreatedAt(LocalDateTime.now());
        
        Quiz savedQuiz = quizRepository.save(quiz);
        
        // Generate questions using AI
        List<Question> questions = questionGenerationService.generateQuestions(
                summary.getSummaryText(),
                difficulty.name(),
                request.getNumberOfQuestions(),
                savedQuiz.getId()
        );
        
        // Save questions
        questionRepository.saveAll(questions);
        
        // Build response WITHOUT correct answers and explanations
        QuizResponse response = new QuizResponse();
        response.setId(savedQuiz.getId());
        response.setSummaryId(savedQuiz.getSummaryId());
        response.setDifficulty(savedQuiz.getDifficulty().name().toLowerCase());
        response.setNumberOfQuestions(savedQuiz.getNumberOfQuestions());
        response.setIsSubmitted(false);
        response.setCreatedAt(savedQuiz.getCreatedAt());
        
        // Map questions (hide correct answers and explanations)
        List<QuestionResponse> questionResponses = questions.stream()
                .map(q -> {
                    try {
                        List<String> options = objectMapper.readValue(
                                q.getOptions(), 
                                new TypeReference<List<String>>() {}
                        );
                        return new QuestionResponse(q.getQuestionId(), q.getQuestionText(), options);
                    } catch (Exception e) {
                        throw new AiServiceException("Failed to parse question options", e);
                    }
                })
                .toList();
        
        response.setQuestions(questionResponses);
        
        log.info("Generated quiz {} with {} questions for user {}", savedQuiz.getId(), questions.size(), userId);
        
        return response;
    }
    
    @Transactional
    public QuizSubmissionResponse submitQuiz(Long userId, Long quizId, QuizSubmission submission) {
        // Find quiz with questions
        Quiz quiz = quizRepository.findByIdAndUserIdWithQuestions(quizId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
        
        // Check if already submitted
        if (quiz.getIsSubmitted()) {
            throw new ValidationException("Quiz has already been submitted");
        }
        
        // Create a map of user answers
        Map<String, String> userAnswersMap = submission.getAnswers().stream()
                .collect(Collectors.toMap(
                        QuizSubmission.QuizAnswer::getQuestionId,
                        QuizSubmission.QuizAnswer::getAnswer
                ));
        
        // Calculate score and build results
        int correctCount = 0;
        List<QuizResultItem> results = new java.util.ArrayList<>();
        
        for (Question question : quiz.getQuestions()) {
            String userAnswer = userAnswersMap.get(question.getQuestionId());
            question.setUserAnswer(userAnswer);
            
            boolean isCorrect = userAnswer != null && 
                    userAnswer.trim().equalsIgnoreCase(question.getCorrectAnswer().trim());
            if (isCorrect) {
                correctCount++;
            }
            
            QuizResultItem resultItem = new QuizResultItem();
            resultItem.setQuestionId(question.getQuestionId());
            resultItem.setQuestion(question.getQuestionText());
            resultItem.setUserAnswer(userAnswer);
            resultItem.setCorrectAnswer(question.getCorrectAnswer());
            resultItem.setIsCorrect(isCorrect);
            resultItem.setExplanation(question.getExplanation());
            
            results.add(resultItem);
        }
        
        // Update quiz
        quiz.setIsSubmitted(true);
        quiz.setCorrectAnswers(correctCount);
        quiz.setSubmittedAt(LocalDateTime.now());
        
        quizRepository.save(quiz);
        questionRepository.saveAll(quiz.getQuestions());
        
        // Build response
        QuizSubmissionResponse response = new QuizSubmissionResponse();
        response.setId(quiz.getId());
        response.setTotalQuestions(quiz.getNumberOfQuestions());
        response.setCorrectAnswers(correctCount);
        response.setSubmittedAt(quiz.getSubmittedAt());
        response.setResults(results);
        
        log.info("Quiz {} submitted by user {} with score {}/{}", 
                quizId, userId, correctCount, quiz.getNumberOfQuestions());
        
        return response;
    }
    
    public Page<QuizListItem> listQuizzes(Long userId, Long summaryId, Pageable pageable) {
        Page<Quiz> quizzes;
        
        if (summaryId != null) {
            // Verify user owns the summary
            if (!summaryRepository.existsByIdAndUserId(summaryId, userId)) {
                throw new ForbiddenException("You do not have access to this summary");
            }
            quizzes = quizRepository.findByUserIdAndSummaryId(userId, summaryId, pageable);
        } else {
            quizzes = quizRepository.findByUserId(userId, pageable);
        }
        
        return quizzes.map(quiz -> {
            QuizListItem item = new QuizListItem();
            item.setId(quiz.getId());
            item.setSummaryId(quiz.getSummaryId());
            
            // Get original filename from summary
            Summary summary = quiz.getSummary();
            if (summary != null) {
                item.setOriginalFilename(summary.getOriginalFilename());
            }
            
            item.setDifficulty(quiz.getDifficulty().name().toLowerCase());
            item.setNumberOfQuestions(quiz.getNumberOfQuestions());
            item.setIsSubmitted(quiz.getIsSubmitted());
            item.setCreatedAt(quiz.getCreatedAt());
            
            if (quiz.getIsSubmitted()) {
                item.setCorrectAnswers(quiz.getCorrectAnswers());
                item.setSubmittedAt(quiz.getSubmittedAt());
            }
            
            return item;
        });
    }
    
    public QuizResponse getQuizDetail(Long userId, Long quizId) {
        Quiz quiz = quizRepository.findByIdAndUserIdWithQuestions(quizId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found"));
        
        QuizResponse response = new QuizResponse();
        response.setId(quiz.getId());
        response.setSummaryId(quiz.getSummaryId());
        response.setDifficulty(quiz.getDifficulty().name().toLowerCase());
        response.setNumberOfQuestions(quiz.getNumberOfQuestions());
        response.setIsSubmitted(quiz.getIsSubmitted());
        response.setCreatedAt(quiz.getCreatedAt());
        
        if (quiz.getIsSubmitted()) {
            // Show full results with correct answers
            response.setCorrectAnswers(quiz.getCorrectAnswers());
            response.setSubmittedAt(quiz.getSubmittedAt());
            
            List<QuizResultItem> results = quiz.getQuestions().stream()
                    .map(q -> {
                        QuizResultItem item = new QuizResultItem();
                        item.setQuestionId(q.getQuestionId());
                        item.setQuestion(q.getQuestionText());
                        item.setUserAnswer(q.getUserAnswer());
                        item.setCorrectAnswer(q.getCorrectAnswer());
                        
                        boolean isCorrect = q.getUserAnswer() != null && 
                                q.getUserAnswer().trim().equalsIgnoreCase(q.getCorrectAnswer().trim());
                        item.setIsCorrect(isCorrect);
                        item.setExplanation(q.getExplanation());
                        
                        return item;
                    })
                    .toList();
            
            response.setResults(results);
        } else {
            // Show only questions without correct answers
            List<QuestionResponse> questionResponses = quiz.getQuestions().stream()
                    .map(q -> {
                        try {
                            List<String> options = objectMapper.readValue(
                                    q.getOptions(), 
                                    new TypeReference<List<String>>() {}
                            );
                            return new QuestionResponse(q.getQuestionId(), q.getQuestionText(), options);
                        } catch (Exception _) {
                            throw new RuntimeException("Failed to parse question options");
                        }
                    })
                    .toList();
            
            response.setQuestions(questionResponses);
        }
        
        return response;
    }
}
