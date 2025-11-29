package ac.inhatc.reservation_system.favorite.controller;

import ac.inhatc.reservation_system.favorite.dto.FavoriteDto;
import ac.inhatc.reservation_system.favorite.service.FavoriteService;
import ac.inhatc.reservation_system.member.entity.Member;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteApiController {

    private final FavoriteService favoriteService;

    /**
     * 찜하기 토글 (추가/삭제)
     */
    @PostMapping("/toggle/{storeId}")
    public ResponseEntity<FavoriteDto.ToggleResponse> toggleFavorite(
            @PathVariable Long storeId,
            HttpServletRequest request
    ) {
        Member member = (Member) request.getAttribute("authenticatedUser");
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            FavoriteDto.ToggleResponse response = favoriteService.toggleFavorite(storeId, member);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("찜 토글 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 찜 상태 확인
     */
    @GetMapping("/status/{storeId}")
    public ResponseEntity<FavoriteDto.StatusResponse> getFavoriteStatus(
            @PathVariable Long storeId,
            HttpServletRequest request
    ) {
        Member member = (Member) request.getAttribute("authenticatedUser");
        // 비로그인 상태도 허용 (찜 개수는 볼 수 있음)

        try {
            FavoriteDto.StatusResponse response = favoriteService.getFavoriteStatus(storeId, member);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("찜 상태 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 내 찜 목록 조회
     */
    @GetMapping("/my")
    public ResponseEntity<List<FavoriteDto.Response>> getMyFavorites(HttpServletRequest request) {
        Member member = (Member) request.getAttribute("authenticatedUser");
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        List<FavoriteDto.Response> favorites = favoriteService.getMyFavorites(member);
        return ResponseEntity.ok(favorites);
    }
}
