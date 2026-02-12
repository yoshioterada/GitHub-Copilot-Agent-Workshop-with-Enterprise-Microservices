package com.skishop.coupon.service;

import com.skishop.coupon.dto.CouponDto;
import com.skishop.coupon.entity.Campaign;
import com.skishop.coupon.entity.Coupon;
import com.skishop.coupon.entity.CouponUsage;
import com.skishop.coupon.exception.CouponException;
import com.skishop.coupon.mapper.CouponMapper;
import com.skishop.coupon.repository.CampaignRepository;
import com.skishop.coupon.repository.CouponRepository;
import com.skishop.coupon.repository.CouponUsageRepository;
import com.skishop.coupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;
    private final CampaignRepository campaignRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponMapper couponMapper;
    private final CouponCodeGeneratorService codeGeneratorService;
    private final CouponValidationService validationService;

    @Transactional
    @CacheEvict(value = "coupons", allEntries = true)
    public CouponDto.CouponResponse createCoupon(CouponDto.CouponRequest request) {
        log.info("Creating coupon with code: {}", request.getCode());

        // Verify campaign exists
        Campaign campaign = campaignRepository.findById(request.getCampaignId())
                .orElseThrow(() -> new CouponException("Campaign not found: " + request.getCampaignId()));

        // Check for duplicate coupon code
        if (couponRepository.existsByCode(request.getCode())) {
            throw new CouponException("Coupon code already exists: " + request.getCode());
        }

        // Check if campaign has reached maximum coupon limit
        if (campaign.hasReachedMaxCoupons()) {
            throw new CouponException("Campaign has reached maximum coupons limit");
        }

        Coupon coupon = Coupon.builder()
                .campaign(campaign)
                .code(request.getCode())
                .couponType(request.getCouponType())
                .discountValue(request.getDiscountValue())
                .discountType(request.getDiscountType())
                .minimumAmount(request.getMinimumAmount())
                .maximumDiscount(request.getMaximumDiscount())
                .usageLimit(request.getUsageLimit())
                .expiresAt(request.getExpiresAt())
                .build();

        coupon = couponRepository.save(coupon);
        campaign.incrementGeneratedCoupons();
        campaignRepository.save(campaign);

        log.info("Created coupon with ID: {}", coupon.getId());
        return couponMapper.toResponse(coupon);
    }

    @Cacheable(value = "coupons", key = "#code")
    public CouponDto.CouponResponse getCouponByCode(String code) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new CouponException("Coupon not found: " + code));
        return couponMapper.toResponse(coupon);
    }

    public Page<CouponDto.CouponResponse> getCouponsByCampaign(UUID campaignId, Pageable pageable) {
        return couponRepository.findByCampaignId(campaignId, pageable)
                .map(couponMapper::toResponse);
    }

    @Transactional
    public CouponDto.CouponValidationResponse validateCoupon(CouponDto.CouponValidationRequest request, UUID userId) {
        try {
            Coupon coupon = couponRepository.findActiveByCode(request.getCode())
                    .orElseThrow(() -> new CouponException("Coupon not found or inactive: " + request.getCode()));

            // Run validation
            validationService.validateCoupon(coupon, userId, request.getCartAmount());

            // Calculate discount
            BigDecimal discount = coupon.calculateDiscount(request.getCartAmount());
            BigDecimal finalAmount = request.getCartAmount().subtract(discount);

            return CouponDto.CouponValidationResponse.builder()
                    .isValid(true)
                    .coupon(couponMapper.toCouponInfo(coupon))
                    .discount(CouponDto.DiscountInfo.builder()
                            .amount(discount)
                            .finalAmount(finalAmount)
                            .build())
                    .build();

        } catch (CouponException e) {
            log.warn("Coupon validation failed: {}", e.getMessage());
            return CouponDto.CouponValidationResponse.builder()
                    .isValid(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Transactional
    @CacheEvict(value = "coupons", key = "#request.code")
    public CouponDto.CouponRedeemResponse redeemCoupon(CouponDto.CouponRedeemRequest request, UUID userId) {
        log.info("Redeeming coupon: {} for user: {}", request.getCode(), userId);

        Coupon coupon = couponRepository.findActiveByCode(request.getCode())
                .orElseThrow(() -> new CouponException("Coupon not found or inactive: " + request.getCode()));

        // Validation
        validationService.validateCouponForRedemption(coupon, userId, request.getOrderAmount(), request.getOrderId());

        // Calculate discount
        BigDecimal discount = coupon.calculateDiscount(request.getOrderAmount());
        BigDecimal finalAmount = request.getOrderAmount().subtract(discount);

        // Create usage record
        CouponUsage usage = CouponUsage.builder()
                .coupon(coupon)
                .userId(userId)
                .orderId(request.getOrderId())
                .discountApplied(discount)
                .orderAmount(request.getOrderAmount())
                .build();

        usage = couponUsageRepository.save(usage);

        // Update coupon usage count
        coupon.incrementUsage();
        couponRepository.save(coupon);

        // Update user coupon status
        userCouponRepository.findByUserIdAndCouponId(userId, coupon.getId())
                .ifPresent(userCoupon -> {
                    userCoupon.markAsRedeemed();
                    userCouponRepository.save(userCoupon);
                });

        log.info("Successfully redeemed coupon: {} with usage ID: {}", request.getCode(), usage.getId());

        return CouponDto.CouponRedeemResponse.builder()
                .usageId(usage.getId())
                .discountApplied(discount)
                .finalAmount(finalAmount)
                .redeemedAt(usage.getUsedAt())
                .build();
    }

    @Transactional
    @CacheEvict(value = "coupons", allEntries = true)
    public CouponDto.BulkCouponResponse generateBulkCoupons(CouponDto.BulkCouponRequest request) {
        log.info("Generating {} bulk coupons for campaign: {}", request.getCount(), request.getCampaignId());

        Campaign campaign = campaignRepository.findById(request.getCampaignId())
                .orElseThrow(() -> new CouponException("Campaign not found: " + request.getCampaignId()));

        // Check if campaign has reached maximum coupon limit
        if (campaign.getMaxCoupons() != null &&
            campaign.getGeneratedCoupons() + request.getCount() > campaign.getMaxCoupons()) {
            throw new CouponException("Bulk generation would exceed campaign's maximum coupons limit");
        }

        List<String> generatedCodes = codeGeneratorService.generateBulkCodes(
                request.getCount(), request.getCodePattern());

        List<Coupon> coupons = generatedCodes.stream()
                .map(code -> Coupon.builder()
                        .campaign(campaign)
                        .code(code)
                        .couponType(request.getCouponConfig().getCouponType())
                        .discountValue(request.getCouponConfig().getDiscountValue())
                        .discountType(Coupon.DiscountType.PERCENTAGE) // Default setting
                        .minimumAmount(request.getCouponConfig().getMinimumAmount())
                        .maximumDiscount(request.getCouponConfig().getMaximumDiscount())
                        .usageLimit(request.getCouponConfig().getUsageLimit())
                        .expiresAt(request.getCouponConfig().getExpiresAt())
                        .build())
                .toList();

        couponRepository.saveAll(coupons);

        // Update campaign's generated coupon count
        campaign.setGeneratedCoupons(campaign.getGeneratedCoupons() + request.getCount());
        campaignRepository.save(campaign);

        UUID batchId = UUID.randomUUID();
        log.info("Generated {} coupons with batch ID: {}", generatedCodes.size(), batchId);

        return CouponDto.BulkCouponResponse.builder()
                .batchId(batchId)
                .campaignId(campaign.getId())
                .requestedCount(request.getCount())
                .generatedCount(generatedCodes.size())
                .generatedCodes(generatedCodes)
                .status("completed")
                .build();
    }

    public List<CouponDto.CouponResponse> getAvailableCouponsByUser(UUID userId) {
        List<Coupon> coupons = couponRepository.findAvailableByUser(userId);
        return coupons.stream()
                .map(couponMapper::toResponse)
                .toList();
    }

    public Page<CouponDto.CouponResponse> getCouponUsage(UUID couponId, Pageable pageable) {
        // TODO: Usage details should be handled by CouponUsageService
        return Page.empty();
    }

        public CouponDto.CouponResponse getCouponById(UUID id) {
                Coupon coupon = couponRepository.findByIdWithUsages(id)
                                .orElseThrow(() -> new CouponException("Coupon not found: " + id));
                return couponMapper.toResponse(coupon);
        }

    @Transactional
    public void expireExpiredCoupons() {
        log.info("Expiring expired coupons");
        List<Coupon> expiredCoupons = couponRepository.findExpiredActiveCoupons(LocalDateTime.now());
        
        expiredCoupons.forEach(coupon -> coupon.setIsActive(false));
        couponRepository.saveAll(expiredCoupons);
        
        log.info("Expired {} coupons", expiredCoupons.size());
    }
}
