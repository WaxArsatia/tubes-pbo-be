package tubes.pbo.be.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tubes.pbo.be.admin.dto.ActivityLogResponse;
import tubes.pbo.be.summary.model.Summary;
import tubes.pbo.be.summary.repository.SummaryRepository;
import tubes.pbo.be.user.model.User;
import tubes.pbo.be.user.repository.UserRepository;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {
    
    private final SummaryRepository summaryRepository;
    private final UserRepository userRepository;
    
    /**
     * Get paginated user activity log showing all summaries with user information
     */
    public Page<ActivityLogResponse> getUserActivity(Pageable pageable) {
        Page<Summary> summaries = summaryRepository.findAllOrderByCreatedAtDesc(pageable);
        
        List<ActivityLogResponse> activityLogs = summaries.getContent().stream()
                .map(summary -> {
                    User user = userRepository.findById(summary.getUserId())
                            .orElse(null);
                    
                    if (user == null) {
                        return null;
                    }
                    
                    return ActivityLogResponse.builder()
                            .userId(user.getId())
                            .userName(user.getName())
                            .userEmail(user.getEmail())
                            .originalFilename(summary.getOriginalFilename())
                            .aiProvider(summary.getAiProvider())
                            .createdAt(summary.getCreatedAt())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        return new PageImpl<>(activityLogs, pageable, summaries.getTotalElements());
    }
}
