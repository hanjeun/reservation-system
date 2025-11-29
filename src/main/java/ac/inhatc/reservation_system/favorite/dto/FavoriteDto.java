package ac.inhatc.reservation_system.favorite.dto;

import ac.inhatc.reservation_system.favorite.entity.Favorite;
import lombok.*;

import java.time.LocalDateTime;

public class FavoriteDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long storeId;
        private String storeName;
        private String storeDescription;
        private String storeMainImageUrl;
        private String storeCategory;
        private Double storeRating;
        private LocalDateTime createdAt;

        public static Response from(Favorite favorite) {
            return Response.builder()
                    .id(favorite.getId())
                    .storeId(favorite.getStore().getId())
                    .storeName(favorite.getStore().getName())
                    .storeDescription(favorite.getStore().getDescription())
                    .storeMainImageUrl(favorite.getStore().getMainImageUrl())
                    .storeCategory(favorite.getStore().getCategory())
                    .storeRating(favorite.getStore().getRating())
                    .createdAt(favorite.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToggleResponse {
        private boolean isFavorite;
        private long favoriteCount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusResponse {
        private boolean isFavorite;
        private long favoriteCount;
    }
}
