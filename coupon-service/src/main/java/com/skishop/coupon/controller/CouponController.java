package com.skishop.coupon.controller;

import com.skishop.coupon.dto.CouponDto;
import com.skishop.coupon.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Coupon Management", description = "Coupon Management API")
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create coupon", description = "Creates a new coupon")
    public ResponseEntity<Map<String, Object>> createCoupon(
            @Valid @RequestBody CouponDto.CouponRequest request) {
        
        log.info("Creating coupon with code: {}", request.getCode());
        CouponDto.CouponResponse response = couponService.createCoupon(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "success", true,
            "data", response
        ));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get coupon list", description = "Retrieves a list of coupons by campaign ID")
    public ResponseEntity<Map<String, Object>> getCoupons(
            @Parameter(description = "Campaign ID") @RequestParam UUID campaignId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<CouponDto.CouponResponse> coupons = couponService.getCouponsByCampaign(campaignId, pageable);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", coupons.getContent(),
            "pagination", Map.of(
                "page", coupons.getNumber(),
                "size", coupons.getSize(),
                "totalElements", coupons.getTotalElements(),
                "totalPages", coupons.getTotalPages()
            )
        ));
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate coupon", description = "Validates the validity of a coupon")
    public ResponseEntity<Map<String, Object>> validateCoupon(
            @Valid @RequestBody CouponDto.CouponValidationRequest request,
            Principal principal) {
        
        UUID userId = UUID.fromString(principal.getName());
        CouponDto.CouponValidationResponse response = couponService.validateCoupon(request, userId);
        
        return ResponseEntity.ok(Map.of(
            "success", response.getIsValid(),
            "data", response
        ));
    }

    @PostMapping("/redeem")
    @Operation(summary = "Use coupon", description = "Use a coupon to apply a discount")
    public ResponseEntity<Map<String, Object>> redeemCoupon(
            @Valid @RequestBody CouponDto.CouponRedeemRequest request,
            Principal principal) {
        
        UUID userId = UUID.fromString(principal.getName());
        CouponDto.CouponRedeemResponse response = couponService.redeemCoupon(request, userId);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", response
        ));
    }

    @GetMapping("/usage/{couponId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get coupon usage status", description = "Retrieves the usage status of the specified coupon")
    public ResponseEntity<Map<String, Object>> getCouponUsage(
            @Parameter(description = "Coupon ID") @PathVariable UUID couponId,
            @PageableDefault(size = 20) Pageable pageable) {
        
        Page<CouponDto.CouponResponse> usage = couponService.getCouponUsage(couponId, pageable);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", usage.getContent(),
            "pagination", Map.of(
                "page", usage.getNumber(),
                "size", usage.getSize(),
                "totalElements", usage.getTotalElements(),
                "totalPages", usage.getTotalPages()
            )
        ));
    }

    @PostMapping("/bulk-generate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk coupon generation", description = "Generates a specified number of coupons in bulk")
    public ResponseEntity<Map<String, Object>> bulkGenerateCoupons(
            @Valid @RequestBody CouponDto.BulkCouponRequest request) {
        
        log.info("Bulk generating {} coupons for campaign: {}", request.getCount(), request.getCampaignId());
        CouponDto.BulkCouponResponse response = couponService.generateBulkCoupons(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "success", true,
            "data", response
        ));
    }

    @GetMapping("/user/available")
    @Operation(summary = "Get available coupons", description = "Retrieves a list of coupons available to the user")
    public ResponseEntity<Map<String, Object>> getAvailableCoupons(Principal principal) {
        
        UUID userId = UUID.fromString(principal.getName());
        List<CouponDto.CouponResponse> coupons = couponService.getAvailableCouponsByUser(userId);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", coupons
        ));
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get coupon details", description = "Retrieves detailed information using coupon code")
    public ResponseEntity<Map<String, Object>> getCouponByCode(
            @Parameter(description = "Coupon code") @PathVariable String code) {
        
        CouponDto.CouponResponse coupon = couponService.getCouponByCode(code);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", coupon
        ));
    }

    @GetMapping("/by-id/{id}")
    @Operation(summary = "Get coupon by ID", description = "Retrieves coupon details by UUID")
    public ResponseEntity<Map<String, Object>> getCouponById(
            @Parameter(description = "Coupon ID") @PathVariable UUID id) {

        CouponDto.CouponResponse coupon = couponService.getCouponById(id);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", coupon
        ));
    }
}
