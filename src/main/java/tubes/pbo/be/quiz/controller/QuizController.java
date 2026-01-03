package tubes.pbo.be.quiz.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tubes.pbo.be.quiz.dto.*;
import tubes.pbo.be.quiz.service.QuizService;
import tubes.pbo.be.shared.dto.ApiResponse;
import tubes.pbo.be.shared.dto.PageResponse;
import tubes.pbo.be.shared.security.SecurityContextHelper;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Quiz", description = "Quiz generation and submission endpoints")
public class QuizController {
    
    private final QuizService quizService;
    private final SecurityContextHelper securityContextHelper;
    
    @PostMapping
    @Operation(
            summary = "Generate a quiz from a summary",
            description = "Creates a new quiz with AI-generated questions based on a summary. " +
                    "Questions are returned without correct answers. Requires summary ownership.",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<QuizResponse>> generateQuiz(
            @Valid @RequestBody QuizRequest request
    ) {
        Long userId = securityContextHelper.getCurrentUserId();
        log.info("User {} generating quiz for summary {}", userId, request.getSummaryId());
        
        QuizResponse quiz = quizService.generateQuiz(userId, request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Quiz generated successfully", quiz));
    }
    
    @PostMapping("/{id}/submit")
    @Operation(
            summary = "Submit quiz answers",
            description = "Submits answers for a quiz and returns results with correct answers and explanations. " +
                    "Quiz can only be submitted once.",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<QuizSubmissionResponse>> submitQuiz(
            @Parameter(description = "Quiz ID") @PathVariable Long id,
            @Valid @RequestBody QuizSubmission submission
    ) {
        Long userId = securityContextHelper.getCurrentUserId();
        log.info("User {} submitting quiz {}", userId, id);
        
        QuizSubmissionResponse result = quizService.submitQuiz(userId, id, submission);
        
        return ResponseEntity.ok(new ApiResponse<>("Quiz submitted successfully", result));
    }
    
    @GetMapping
    @Operation(
            summary = "List user's quizzes",
            description = "Returns a paginated list of quizzes for the current user. " +
                    "Optional summaryId filter to show quizzes for a specific summary.",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<PageResponse<QuizListItem>> listQuizzes(
            @Parameter(description = "Filter by summary ID (optional)")
            @RequestParam(required = false) Long summaryId,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = securityContextHelper.getCurrentUserId();
        log.info("User {} listing quizzes (summaryId: {})", userId, summaryId);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<QuizListItem> quizzes = quizService.listQuizzes(userId, summaryId, pageable);
        
        PageResponse<QuizListItem> response = new PageResponse<>(
                quizzes.getContent(),
                quizzes.getNumber(),
                quizzes.getSize(),
                quizzes.getTotalElements(),
                quizzes.getTotalPages()
        );
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    @Operation(
            summary = "Get quiz details",
            description = "Returns quiz details. If not submitted, shows questions without answers. " +
                    "If submitted, shows full results with correct answers and explanations.",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    public ResponseEntity<ApiResponse<QuizResponse>> getQuizDetail(
            @Parameter(description = "Quiz ID") @PathVariable Long id
    ) {
        Long userId = securityContextHelper.getCurrentUserId();
        log.info("User {} retrieving quiz {}", userId, id);
        
        QuizResponse quiz = quizService.getQuizDetail(userId, id);
        
        return ResponseEntity.ok(new ApiResponse<>("Quiz retrieved successfully", quiz));
    }
}
