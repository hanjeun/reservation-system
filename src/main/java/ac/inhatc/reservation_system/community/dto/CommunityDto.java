package ac.inhatc.reservation_system.community.dto;

import ac.inhatc.reservation_system.community.entity.CommunityPost;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CommunityDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PostRequest {
        private String title;
        private String content;
        private String category;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PostResponse {
        private Long id;
        private String title;
        private String content;
        private String category;
        private String categoryDisplayName;
        private String authorName;
        private Long authorId;
        private Integer viewCount;
        private Integer likeCount;
        private Integer commentCount;
        private String createdAt;
        private String updatedAt;

        public static PostResponse from(CommunityPost post) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            
            return PostResponse.builder()
                    .id(post.getId())
                    .title(post.getTitle())
                    .content(post.getContent())
                    .category(post.getCategory().name())
                    .categoryDisplayName(post.getCategory().getDisplayName())
                    .authorName(post.getAuthor().getName())
                    .authorId(post.getAuthor().getId())
                    .viewCount(post.getViewCount())
                    .likeCount(post.getLikeCount())
                    .commentCount(post.getComments().size())
                    .createdAt(post.getCreatedAt().format(formatter))
                    .updatedAt(post.getUpdatedAt().format(formatter))
                    .isLiked(false)
                    .isAuthor(false)
                    .build();
        }

        public static PostResponse from(CommunityPost post, Long currentUserId, boolean isLiked) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            
            return PostResponse.builder()
                    .id(post.getId())
                    .title(post.getTitle())
                    .content(post.getContent())
                    .category(post.getCategory().name())
                    .categoryDisplayName(post.getCategory().getDisplayName())
                    .authorName(post.getAuthor().getName())
                    .authorId(post.getAuthor().getId())
                    .viewCount(post.getViewCount())
                    .likeCount(post.getLikeCount())
                    .commentCount(post.getComments().size())
                    .createdAt(post.getCreatedAt().format(formatter))
                    .updatedAt(post.getUpdatedAt().format(formatter))
                    .isLiked(isLiked)
                    .isAuthor(post.getAuthor().getId().equals(currentUserId))
                    .build();
        }

        @JsonProperty("isLiked")
        private boolean isLiked;

        @JsonProperty("isAuthor")
        private boolean isAuthor;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommentRequest {
        private String content;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CommentResponse {
        private Long id;
        private String content;
        private String authorName;
        private Long authorId;
        private String createdAt;
        private String updatedAt;

        public static CommentResponse from(ac.inhatc.reservation_system.community.entity.CommunityComment comment, Long currentUserId) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            
            return CommentResponse.builder()
                    .id(comment.getId())
                    .content(comment.getContent())
                    .authorName(comment.getAuthor().getName())
                    .authorId(comment.getAuthor().getId())
                    .createdAt(comment.getCreatedAt().format(formatter))
                    .updatedAt(comment.getUpdatedAt().format(formatter))
                    .isAuthor(comment.getAuthor().getId().equals(currentUserId))
                    .build();
        }

        @JsonProperty("isAuthor")
        private boolean isAuthor;
    }
}
