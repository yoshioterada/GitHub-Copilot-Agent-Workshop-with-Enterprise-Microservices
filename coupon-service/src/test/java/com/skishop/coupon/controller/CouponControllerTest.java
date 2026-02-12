package com.skishop.coupon.controller;

import com.skishop.coupon.dto.CouponDto;
import com.skishop.coupon.exception.CouponException;
import com.skishop.coupon.service.CouponService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponControllerTest {

    @Mock
    CouponService couponService;

    @InjectMocks
    CouponController couponController;

    @Test
    @DisplayName("getCouponByCode: returns 200 and body when coupon exists")
    void getCouponByCode_found() {
        UUID id = UUID.randomUUID();
        CouponDto.CouponResponse resp = CouponDto.CouponResponse.builder()
                .id(id)
                .campaignId(UUID.randomUUID())
                .code("TESTCODE")
                .discountValue(new BigDecimal("10.00"))
                .discountType(null)
                .isActive(true)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(couponService.getCouponByCode(anyString())).thenReturn(resp);

        var response = couponController.getCouponByCode("TESTCODE");

        assertEquals(200, response.getStatusCodeValue());
        Map body = response.getBody();
        assertNotNull(body);
        assertTrue(Boolean.TRUE.equals(body.get("success")));
        Object data = body.get("data");
        assertNotNull(data);
        assertTrue(data instanceof CouponDto.CouponResponse);
        assertEquals("TESTCODE", ((CouponDto.CouponResponse) data).getCode());
    }

    @Test
    @DisplayName("getCouponByCode: propagates exception when service throws")
    void getCouponByCode_notFound() {
        when(couponService.getCouponByCode(anyString())).thenThrow(new CouponException("Coupon not found"));

        assertThrows(CouponException.class, () -> couponController.getCouponByCode("NOPE"));
    }

    @Test
    @DisplayName("getCouponById: returns coupon response when found")
    void getCouponById_found() {
        UUID id = UUID.randomUUID();
        CouponDto.CouponResponse resp = CouponDto.CouponResponse.builder()
                .id(id)
                .campaignId(UUID.randomUUID())
                .code("IDCODE")
                .discountValue(new BigDecimal("5.00"))
                .isActive(true)
                .expiresAt(LocalDateTime.now().plusDays(3))
                .build();

        when(couponService.getCouponById(id)).thenReturn(resp);

        var response = couponController.getCouponById(id);

        assertEquals(200, response.getStatusCodeValue());
        Map body = response.getBody();
        assertNotNull(body);
        assertTrue(Boolean.TRUE.equals(body.get("success")));
        assertEquals("IDCODE", ((CouponDto.CouponResponse) body.get("data")).getCode());
    }

    @Test
    @DisplayName("getCouponById: throws when not found")
    void getCouponById_notFound() {
        UUID id = UUID.randomUUID();
        when(couponService.getCouponById(id)).thenThrow(new CouponException("not found"));

        assertThrows(CouponException.class, () -> couponController.getCouponById(id));
    }
}
