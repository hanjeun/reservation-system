package ac.inhatc.reservation_system.reservation.service;

import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.reservation.dto.ReservationCreateRequest;
import ac.inhatc.reservation_system.reservation.dto.ReservationResponse;
import ac.inhatc.reservation_system.reservation.dto.ReservationUpdateRequest;
import ac.inhatc.reservation_system.reservation.entity.Reservation;
import ac.inhatc.reservation_system.reservation.repository.ReservationRepository;
import ac.inhatc.reservation_system.review.entity.Review;
import ac.inhatc.reservation_system.review.repository.ReviewRepository;
import ac.inhatc.reservation_system.store.repository.StoreRepository;
import ac.inhatc.reservation_system.store.entity.Store;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final StoreRepository storeRepository;
    private final ReviewRepository reviewRepository;

    /**
     * 예약 생성
     */
    @Transactional
    public ReservationResponse createReservation(ReservationCreateRequest request, Member member) {
        log.info("📝 예약 생성 시작: storeId={}, memberId={}", request.getStoreId(), member.getId());

        // 가게 조회
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("가게를 찾을 수 없습니다."));

        // 예약 날짜/시간 검증
        LocalDateTime reservationDateTime = LocalDateTime.of(
                request.getReservationDate(),
                request.getReservationTime()
        );

        if (reservationDateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("예약 날짜/시간은 현재 이후여야 합니다.");
        }

        // 예약 생성
        Reservation reservation = Reservation.builder()
                .member(member)
                .store(store)
                .reservationDate(request.getReservationDate())
                .reservationTime(request.getReservationTime())
                .guestCount(request.getGuestCount())
                .specialRequest(request.getSpecialRequest())
                .status(Reservation.ReservationStatus.PENDING)
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);
        log.info("✅ 예약 생성 완료: reservationId={}", savedReservation.getId());

        return ReservationResponse.from(savedReservation);
    }

    /**
     * 내 예약 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getMyReservations(Member member) {
        log.info("📋 내 예약 목록 조회: memberId={}", member.getId());

        List<Reservation> reservations = reservationRepository.findByMemberOrderByCreatedAtDesc(member);

        return reservations.stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 예약 상세 조회
     */
    @Transactional(readOnly = true)
    public ReservationResponse getReservation(Long id, Member member) {
        log.info("📋 예약 상세 조회: reservationId={}, memberId={}", id, member.getId());

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

        // 본인 예약인지 확인
        if (!reservation.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("본인의 예약만 조회할 수 있습니다.");
        }

        return ReservationResponse.from(reservation);
    }

    /**
     * 예약 수정
     */
    @Transactional
    public ReservationResponse updateReservation(Long id, ReservationUpdateRequest request, Member member) {
        log.info("✏️ 예약 수정: reservationId={}, memberId={}", id, member.getId());

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

        // 본인 예약인지 확인
        if (!reservation.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("본인의 예약만 수정할 수 있습니다.");
        }

        // 취소된 예약은 수정 불가
        if (reservation.getStatus() == Reservation.ReservationStatus.CANCELLED) {
            throw new IllegalArgumentException("취소된 예약은 수정할 수 없습니다.");
        }

        // 예약 정보 수정
        if (request.getReservationDate() != null) {
            reservation.setReservationDate(request.getReservationDate());
        }
        if (request.getReservationTime() != null) {
            reservation.setReservationTime(request.getReservationTime());
        }
        if (request.getGuestCount() != null) {
            reservation.setGuestCount(request.getGuestCount());
        }
        if (request.getSpecialRequest() != null) {
            reservation.setSpecialRequest(request.getSpecialRequest());
        }
        if (request.getStatus() != null) {
            reservation.setStatus(request.getStatus());
        }

        // 날짜/시간이 수정된 경우 검증
        LocalDateTime reservationDateTime = LocalDateTime.of(
                reservation.getReservationDate(),
                reservation.getReservationTime()
        );

        if (reservationDateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("예약 날짜/시간은 현재 이후여야 합니다.");
        }

        Reservation updatedReservation = reservationRepository.save(reservation);
        log.info("✅ 예약 수정 완료: reservationId={}", updatedReservation.getId());

        return ReservationResponse.from(updatedReservation);
    }

    /**
     * 예약 취소
     */
    @Transactional
    public void cancelReservation(Long id, Member member) {
        log.info("❌ 예약 취소: reservationId={}, memberId={}", id, member.getId());

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

        // 본인 예약인지 확인
        if (!reservation.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("본인의 예약만 취소할 수 있습니다.");
        }

        // 이미 취소된 예약인지 확인
        if (reservation.getStatus() == Reservation.ReservationStatus.CANCELLED) {
            throw new IllegalArgumentException("이미 취소된 예약입니다.");
        }

        // 예약 상태를 취소로 변경
        reservation.setStatus(Reservation.ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        log.info("✅ 예약 취소 완료: reservationId={}", id);
    }

    /**
     * 예약 삭제 (완전 삭제)
     */
    @Transactional
    public void deleteReservation(Long id, Member member) {
        log.info("🗑️ 예약 삭제: reservationId={}, memberId={}", id, member.getId());

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

        // 권한 확인: 본인 예약이거나 가게 소유자인 경우만 삭제 가능
        boolean isOwner = reservation.getMember().getId().equals(member.getId());
        boolean isStoreOwner = reservation.getStore().getOwner().getId().equals(member.getId());
        
        if (!isOwner && !isStoreOwner) {
            throw new IllegalArgumentException("본인의 예약이거나 본인 가게의 예약만 삭제할 수 있습니다.");
        }

        // 해당 예약에 연결된 리뷰가 있으면 예약 참조를 null로 설정 (리뷰는 유지)
        reviewRepository.findByReservationId(id).ifPresent(review -> {
            review.setReservation(null);
            reviewRepository.save(review);
            log.info("📝 리뷰의 예약 참조 해제: reviewId={}", review.getId());
        });

        reservationRepository.delete(reservation);
        log.info("✅ 예약 삭제 완료: reservationId={}", id);
    }
    
    // ========== 사업자용 메서드 ==========
    
    /**
     * 내 가게의 예약 목록 조회 (사업자용)
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getStoreReservations(Member owner) {
        log.info("📋 가게 예약 목록 조회: ownerId={}", owner.getId());
        
        // 사업자가 소유한 모든 가게 조회
        List<Store> stores = storeRepository.findByOwnerOrderByCreatedAtDesc(owner);
        
        // 각 가게의 예약 목록 조회
        List<Reservation> reservations = stores.stream()
                .flatMap(store -> reservationRepository.findByStoreOrderByCreatedAtDesc(store).stream())
                .collect(Collectors.toList());
        
        log.info("✅ 예약 목록 조회 완료: {}개", reservations.size());
        
        return reservations.stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 예약 승인 (사업자용)
     */
    @Transactional
    public void approveReservation(Long id, Member owner) {
        log.info("✅ 예약 승인: reservationId={}, ownerId={}", id, owner.getId());
        
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));
        
        // 가게 소유자 확인
        if (!reservation.getStore().getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("본인 가게의 예약만 승인할 수 있습니다.");
        }
        
        // 대기중 상태인지 확인
        if (reservation.getStatus() != Reservation.ReservationStatus.PENDING) {
            throw new IllegalArgumentException("대기중인 예약만 승인할 수 있습니다.");
        }
        
        reservation.setStatus(Reservation.ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);
        
        log.info("✅ 예약 승인 완료: reservationId={}", id);
    }
    
    /**
     * 예약 거절 (사업자용)
     */
    @Transactional
    public void rejectReservation(Long id, Member owner, String rejectionReason) {
        log.info("❌ 예약 거절: reservationId={}, ownerId={}, reason={}", id, owner.getId(), rejectionReason);
        
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));
        
        // 가게 소유자 확인
        if (!reservation.getStore().getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("본인 가게의 예약만 거절할 수 있습니다.");
        }
        
        // 대기중 상태인지 확인
        if (reservation.getStatus() != Reservation.ReservationStatus.PENDING) {
            throw new IllegalArgumentException("대기중인 예약만 거절할 수 있습니다.");
        }
        
        reservation.setStatus(Reservation.ReservationStatus.REJECTED);
        
        // 거절 사유는 선택사항 (빈 문자열이나 null이어도 허용)
        if (rejectionReason != null && !rejectionReason.trim().isEmpty()) {
            reservation.setRejectionReason(rejectionReason);
        } else {
            reservation.setRejectionReason("거절 사유 없음");
        }
        
        reservationRepository.save(reservation);
        
        log.info("✅ 예약 거절 완료: reservationId={}", id);
    }
    
    /**
     * 예약 취소 (사업자용 - 승인된 예약도 취소 가능)
     */
    @Transactional
    public void cancelReservationByOwner(Long id, Member owner) {
        log.info("❌ 사업자 예약 취소: reservationId={}, ownerId={}", id, owner.getId());
        
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));
        
        // 가게 소유자 확인
        if (!reservation.getStore().getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("본인 가게의 예약만 취소할 수 있습니다.");
        }
        
        // 이미 취소/거절된 예약인지 확인
        if (reservation.getStatus() == Reservation.ReservationStatus.CANCELLED ||
            reservation.getStatus() == Reservation.ReservationStatus.REJECTED) {
            throw new IllegalArgumentException("이미 취소되거나 거절된 예약입니다.");
        }
        
        reservation.setStatus(Reservation.ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        
        log.info("✅ 사업자 예약 취소 완료: reservationId={}", id);
    }

    /**
     * 이용완료 처리 (사업자용)
     */
    @Transactional
    public void completeReservation(Long id, Member owner) {
        log.info("✅ 이용완료 처리: reservationId={}, ownerId={}", id, owner.getId());

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

        // 가게 소유자 확인
        if (!reservation.getStore().getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("본인 가게의 예약만 이용완료 처리할 수 있습니다.");
        }

        // 승인된 상태인지 확인
        if (reservation.getStatus() != Reservation.ReservationStatus.CONFIRMED) {
            throw new IllegalArgumentException("승인된 예약만 이용완료 처리할 수 있습니다.");
        }

        reservation.setStatus(Reservation.ReservationStatus.COMPLETED);
        reservationRepository.save(reservation);

        log.info("✅ 이용완료 처리 완료: reservationId={}", id);
    }
}
