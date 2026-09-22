package com.nguyenquyen.vetautet.ddd.infrastructure.payment;

import com.nguyenquyen.vetautet.ddd.infrastructure.config.VNPayConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class VNPayServiceImpl implements PaymentGatewayService {

    @Value("${vnpay.tmn-code}")
    private String vnpTmnCode;

    @Value("${vnpay.hash-secret}")
    private String vnpHashSecret;

    @Value("${vnpay.url}")
    private String vnpUrl;

    @Value("${vnpay.return-url}")
    private String vnpReturnUrl;

    @Override
    public String createPaymentUrl(String orderNumber, BigDecimal amount, String clientIp) {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        
        long amountInVND = amount.longValue() * 100; // VNPay requires amount * 100

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnpTmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amountInVND));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", orderNumber);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + orderNumber);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnpReturnUrl);
        vnp_Params.put("vnp_IpAddr", clientIp);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        try {
            StringBuilder sb = new StringBuilder();
            for (String fieldName : fieldNames) {
                String fieldValue = vnp_Params.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    if (!sb.isEmpty()) {
                        sb.append("&");
                    }
                    sb.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    sb.append("=");
                    sb.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                }
            }
            
            String queryUrl = sb.toString();
            String vnp_SecureHash = VNPayConfig.hmacSHA512(vnpHashSecret, queryUrl);
            queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
            String finalUrl = vnpUrl + "?" + queryUrl;
            
            log.info("============== VNPAY URL ==============");
            log.info(finalUrl);
            log.info("=======================================");
            
            return finalUrl;
        } catch (Exception e) {
            log.error("Error creating VNPay URL", e);
            throw new RuntimeException("Lỗi tạo URL thanh toán VNPay", e);
        }
    }

    @Override
    public boolean verifyPayment(Map<String, String> params) {
        String vnp_SecureHash = params.get("vnp_SecureHash");
        if (vnp_SecureHash == null) {
            return false;
        }
        
        // Build sorted and URL-encoded query string for hashing
        Map<String, String> signParams = new TreeMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey().startsWith("vnp_") && !entry.getKey().equals("vnp_SecureHash") && !entry.getKey().equals("vnp_SecureHashType")) {
                signParams.put(entry.getKey(), entry.getValue());
            }
        }
        
        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : signParams.entrySet()) {
                String fieldValue = entry.getValue();
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    if (!sb.isEmpty()) sb.append("&");
                    sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII.toString()));
                    sb.append("=");
                    sb.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                }
            }
            
            String signValue = VNPayConfig.hmacSHA512(vnpHashSecret, sb.toString());
            return signValue.equals(vnp_SecureHash);
        } catch (Exception e) {
            log.error("Error verifying VNPay signature", e);
            return false;
        }
    }
}
