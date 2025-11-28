package ac.inhatc.reservation_system.customerservice.service;

import ac.inhatc.reservation_system.customerservice.dto.InquiryDto;
import ac.inhatc.reservation_system.customerservice.entity.Inquiry;
import ac.inhatc.reservation_system.customerservice.repository.InquiryRepository;
import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final MemberRepository memberRepository;

    // 내 문의 목록 조회 (페이징)
    public Page<InquiryDto.InquiryResponse> getMyInquiries(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Inquiry> inquiries = inquiryRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
        return inquiries.map(InquiryDto.InquiryResponse::from);
    }

    // 전체 문의 목록 조회 (관리자용)
    public Page<InquiryDto.InquiryResponse> getAllInquiries(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Inquiry> inquiries = inquiryRepository.findAllByOrderByCreatedAtDesc(pageable);
        return inquiries.map(InquiryDto.InquiryResponse::from);
    }

    // 문의 상세 조회
    public InquiryDto.InquiryResponse getInquiry(Long inquiryId, Long memberId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("문의를 찾을 수 없습니다."));

        // 본인의 문의만 조회 가능
        if (!inquiry.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 문의만 조회할 수 있습니다.");
        }

        return InquiryDto.InquiryResponse.from(inquiry);
    }

    // 문의 상세 조회 (관리자용 - 권한 체크 없음)
    public InquiryDto.InquiryResponse getInquiryForAdmin(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("문의를 찾을 수 없습니다."));

        return InquiryDto.InquiryResponse.from(inquiry);
    }

    // 문의 작성
    @Transactional
    public InquiryDto.InquiryResponse createInquiry(Long memberId, InquiryDto.InquiryRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        Inquiry inquiry = Inquiry.builder()
                .member(member)
                .category(Inquiry.InquiryCategory.valueOf(request.getCategory()))
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        Inquiry savedInquiry = inquiryRepository.save(inquiry);
        return InquiryDto.InquiryResponse.from(savedInquiry);
    }

    // 문의 삭제
    @Transactional
    public void deleteInquiry(Long inquiryId, Long memberId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("문의를 찾을 수 없습니다."));

        if (!inquiry.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 문의만 삭제할 수 있습니다.");
        }

        // 답변이 완료된 문의는 삭제 불가
        if (inquiry.getStatus() == Inquiry.InquiryStatus.ANSWERED) {
            throw new IllegalArgumentException("답변이 완료된 문의는 삭제할 수 없습니다.");
        }

        inquiryRepository.delete(inquiry);
    }

    // 답변 작성 (관리자용)
    @Transactional
    public InquiryDto.InquiryResponse answerInquiry(Long inquiryId, InquiryDto.AnswerRequest request) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("문의를 찾을 수 없습니다."));

        inquiry.answer(request.getAnswer());

        return InquiryDto.InquiryResponse.from(inquiry);
    }

    // 미답변 문의 개수
    public Long getPendingInquiryCount(Long memberId) {
        return inquiryRepository.countByMemberIdAndStatus(memberId, Inquiry.InquiryStatus.PENDING);
    }

    // 전체 미답변 문의 개수 (관리자용)
    public Long getTotalPendingInquiryCount() {
        Pageable pageable = PageRequest.of(0, 1);
        return inquiryRepository.findByStatusOrderByCreatedAtDesc(Inquiry.InquiryStatus.PENDING, pageable).getTotalElements();
    }

    // 문의 삭제 (관리자용 - 답변 완료된 것도 삭제 가능)
    @Transactional
    public void deleteInquiryAsAdmin(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("문의를 찾을 수 없습니다."));

        inquiryRepository.delete(inquiry);
    }
}
