/**
 * 포트원 결제 연동 JavaScript
 * 카카오페이 간편결제 지원
 */

// 포트원 초기화 여부
let isPortoneInitialized = false;

/**
 * 포트원 SDK 초기화
 */
async function initPortone() {
    if (isPortoneInitialized) return;

    try {
        // 포트원 설정 정보 가져오기
        const response = await fetch('/api/payment/config');
        const config = await response.json();

        if (typeof IMP === 'undefined') {
            console.error('포트원 SDK가 로드되지 않았습니다.');
            return;
        }

        IMP.init(config.impCode);
        isPortoneInitialized = true;
        console.log('포트원 SDK 초기화 완료');
    } catch (error) {
        console.error('포트원 초기화 실패:', error);
    }
}

/**
 * 결제 요청 (카카오페이)
 * @param {Object} options - 결제 옵션
 * @param {number} options.reservationId - 예약 ID
 * @param {number} options.amount - 결제 금액
 * @param {string} options.productName - 상품명
 * @param {string} options.buyerName - 구매자 이름
 * @param {string} options.buyerEmail - 구매자 이메일
 * @param {string} options.buyerTel - 구매자 전화번호
 * @param {boolean} options.redirectToResult - 결과 페이지로 리다이렉트 여부 (기본: true)
 * @returns {Promise} 결제 결과
 */
async function requestPayment(options) {
    await initPortone();

    const { 
        reservationId, 
        amount, 
        productName, 
        buyerName, 
        buyerEmail, 
        buyerTel,
        redirectToResult = true  // 기본적으로 결과 페이지로 이동
    } = options;

    try {
        // 1. 서버에서 결제 준비 (merchant_uid 생성)
        const prepareResponse = await window.fetchWithAuth('/api/payment/prepare', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                reservationId,
                amount,
                productName,
                buyerName,
                buyerEmail,
                buyerTel,
                pgProvider: 'kakaopay'
            })
        });

        if (!prepareResponse.ok) {
            const errorText = await prepareResponse.text();
            throw new Error(errorText || '결제 준비 실패');
        }

        const prepareData = await prepareResponse.json();

        // 2. 포트원 결제창 호출
        return new Promise((resolve, reject) => {
            IMP.request_pay({
                pg: 'kakaopay',
                pay_method: 'card',
                merchant_uid: prepareData.merchantUid,
                name: prepareData.productName,
                amount: prepareData.amount,
                buyer_email: prepareData.buyerEmail,
                buyer_name: prepareData.buyerName,
                buyer_tel: prepareData.buyerTel,
                m_redirect_url: window.location.origin + '/payment/mobile-redirect'
            }, async function(response) {
                if (response.success) {
                    try {
                        // 3. 결제 검증
                        const verifyResponse = await window.fetchWithAuth('/api/payment/verify', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json',
                            },
                            body: JSON.stringify({
                                impUid: response.imp_uid,
                                merchantUid: response.merchant_uid,
                                reservationId: reservationId
                            })
                        });

                        if (verifyResponse.ok) {
                            const verifyData = await verifyResponse.json();
                            
                            // 결과 페이지로 리다이렉트
                            if (redirectToResult) {
                                const resultUrl = `/payment/result?success=true` +
                                    `&merchant_uid=${encodeURIComponent(response.merchant_uid)}` +
                                    `&reservation_id=${reservationId}` +
                                    `&message=${encodeURIComponent('결제가 완료되었습니다.')}` +
                                    `&reason=${encodeURIComponent('노쇼 방지금이 결제되었습니다. 정상 방문 시 전액 환불됩니다.')}`;
                                window.location.href = resultUrl;
                            } else {
                                resolve({
                                    success: true,
                                    message: '결제가 완료되었습니다.',
                                    data: verifyData
                                });
                            }
                        } else {
                            const errorText = await verifyResponse.text();
                            
                            if (redirectToResult) {
                                const resultUrl = `/payment/failed` +
                                    `?merchant_uid=${encodeURIComponent(response.merchant_uid)}` +
                                    `&error_msg=${encodeURIComponent('결제 검증에 실패했습니다.')}`;
                                window.location.href = resultUrl;
                            } else {
                                reject({
                                    success: false,
                                    message: '결제 검증에 실패했습니다.'
                                });
                            }
                        }
                    } catch (error) {
                        if (redirectToResult) {
                            const resultUrl = `/payment/failed` +
                                `?merchant_uid=${encodeURIComponent(response.merchant_uid)}` +
                                `&error_msg=${encodeURIComponent('결제 검증 중 오류가 발생했습니다.')}`;
                            window.location.href = resultUrl;
                        } else {
                            reject({
                                success: false,
                                message: '결제 검증 중 오류가 발생했습니다.',
                                error: error
                            });
                        }
                    }
                } else {
                    // 결제 실패 또는 취소
                    const errorMsg = response.error_msg || '결제가 취소되었습니다.';
                    const isCancelled = errorMsg.includes('취소') || errorMsg.includes('cancel');
                    
                    // 서버에서 결제 취소 처리
                    window.fetchWithAuth(`/api/payment/cancel/${prepareData.merchantUid}`, {
                        method: 'POST'
                    }).catch(err => console.log('결제 취소 처리:', err));

                    if (redirectToResult) {
                        if (isCancelled) {
                            const resultUrl = `/payment/cancelled` +
                                `?merchant_uid=${encodeURIComponent(prepareData.merchantUid)}` +
                                `&reason=${encodeURIComponent(errorMsg)}`;
                            window.location.href = resultUrl;
                        } else {
                            const resultUrl = `/payment/failed` +
                                `?merchant_uid=${encodeURIComponent(prepareData.merchantUid)}` +
                                `&error_msg=${encodeURIComponent(errorMsg)}` +
                                (response.error_code ? `&error_code=${encodeURIComponent(response.error_code)}` : '');
                            window.location.href = resultUrl;
                        }
                    } else {
                        reject({
                            success: false,
                            message: errorMsg,
                            isCancelled: isCancelled
                        });
                    }
                }
            });
        });
    } catch (error) {
        console.error('결제 요청 실패:', error);
        throw error;
    }
}

/**
 * 카카오페이 결제
 */
async function payWithKakaoPay(options) {
    return requestPayment(options);
}

/**
 * 결제 환불 요청
 * @param {Object} options - 환불 옵션
 * @param {number} options.paymentId - 결제 ID
 * @param {number} options.reservationId - 예약 ID (paymentId 없을 경우)
 * @param {number} options.refundAmount - 환불 금액 (부분 환불 시)
 * @param {string} options.refundReason - 환불 사유
 * @returns {Promise} 환불 결과
 */
async function requestRefund(options) {
    const { paymentId, reservationId, refundAmount, refundReason } = options;

    try {
        const response = await window.fetchWithAuth('/api/payment/refund', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                paymentId,
                reservationId,
                refundAmount,
                refundReason
            })
        });

        if (response.ok) {
            const data = await response.json();
            return {
                success: true,
                message: '환불이 완료되었습니다.',
                data: data
            };
        } else {
            throw new Error('환불 요청 실패');
        }
    } catch (error) {
        console.error('환불 요청 실패:', error);
        return {
            success: false,
            message: '환불 요청 중 오류가 발생했습니다.',
            error: error
        };
    }
}

/**
 * 결제 정보 조회
 * @param {number} reservationId - 예약 ID
 * @returns {Promise} 결제 정보
 */
async function getPaymentInfo(reservationId) {
    try {
        const response = await window.fetchWithAuth(`/api/payment/reservation/${reservationId}`);
        
        if (response.ok) {
            return await response.json();
        } else {
            return null;
        }
    } catch (error) {
        console.error('결제 정보 조회 실패:', error);
        return null;
    }
}

/**
 * 결제 금액 포맷팅
 * @param {number} amount - 금액
 * @returns {string} 포맷팅된 금액
 */
function formatAmount(amount) {
    return new Intl.NumberFormat('ko-KR').format(amount) + '원';
}

/**
 * 결제 상태 한글 변환
 * @param {string} status - 결제 상태
 * @returns {string} 한글 상태
 */
function getPaymentStatusText(status) {
    const statusMap = {
        'READY': '결제 대기',
        'PAID': '결제 완료',
        'CANCELLED': '결제 취소',
        'FAILED': '결제 실패',
        'REFUNDED': '환불 완료',
        'PARTIAL_REFUNDED': '부분 환불'
    };
    return statusMap[status] || status;
}

/**
 * 결제 상태에 따른 배지 클래스
 * @param {string} status - 결제 상태
 * @returns {string} 배지 클래스
 */
function getPaymentStatusBadgeClass(status) {
    const classMap = {
        'READY': 'badge-warning',
        'PAID': 'badge-success',
        'CANCELLED': 'badge-secondary',
        'FAILED': 'badge-danger',
        'REFUNDED': 'badge-info',
        'PARTIAL_REFUNDED': 'badge-info'
    };
    return classMap[status] || 'badge-secondary';
}

// 페이지 로드 시 포트원 초기화
document.addEventListener('DOMContentLoaded', function() {
    // 포트원 SDK가 로드되면 초기화
    if (typeof IMP !== 'undefined') {
        initPortone();
    }
});
