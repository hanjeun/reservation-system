package ac.inhatc.reservation_system.favorite.service;

import ac.inhatc.reservation_system.favorite.dto.FavoriteDto;
import ac.inhatc.reservation_system.favorite.entity.Favorite;
import ac.inhatc.reservation_system.favorite.repository.FavoriteRepository;
import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.store.entity.Store;
import ac.inhatc.reservation_system.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final StoreRepository storeRepository;

    /**
     * 찜하기 토글 (추가/삭제)
     */
    @Transactional
    public FavoriteDto.ToggleResponse toggleFavorite(Long storeId, Member member) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("가게를 찾을 수 없습니다: " + storeId));

        boolean isFavorite;
        
        // 이미 찜한 경우 -> 삭제
        if (favoriteRepository.existsByMemberAndStore(member, store)) {
            Favorite favorite = favoriteRepository.findByMemberAndStore(member, store)
                    .orElseThrow(() -> new IllegalStateException("찜 정보를 찾을 수 없습니다"));
            favoriteRepository.delete(favorite);
            isFavorite = false;
            log.info("찜 삭제: 회원={}, 가게={}", member.getEmail(), store.getName());
        } 
        // 찜하지 않은 경우 -> 추가
        else {
            Favorite favorite = Favorite.builder()
                    .member(member)
                    .store(store)
                    .build();
            favoriteRepository.save(favorite);
            isFavorite = true;
            log.info("찜 추가: 회원={}, 가게={}", member.getEmail(), store.getName());
        }

        long favoriteCount = favoriteRepository.countByStore(store);

        return FavoriteDto.ToggleResponse.builder()
                .isFavorite(isFavorite)
                .favoriteCount(favoriteCount)
                .build();
    }

    /**
     * 찜 상태 확인
     */
    public FavoriteDto.StatusResponse getFavoriteStatus(Long storeId, Member member) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("가게를 찾을 수 없습니다: " + storeId));

        boolean isFavorite = member != null && favoriteRepository.existsByMemberAndStore(member, store);
        long favoriteCount = favoriteRepository.countByStore(store);

        return FavoriteDto.StatusResponse.builder()
                .isFavorite(isFavorite)
                .favoriteCount(favoriteCount)
                .build();
    }

    /**
     * 내 찜 목록 조회
     */
    public List<FavoriteDto.Response> getMyFavorites(Member member) {
        return favoriteRepository.findByMemberOrderByCreatedAtDesc(member)
                .stream()
                .map(FavoriteDto.Response::from)
                .collect(Collectors.toList());
    }

    /**
     * 가게의 찜 개수 조회
     */
    public long getFavoriteCount(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("가게를 찾을 수 없습니다: " + storeId));
        return favoriteRepository.countByStore(store);
    }
}
