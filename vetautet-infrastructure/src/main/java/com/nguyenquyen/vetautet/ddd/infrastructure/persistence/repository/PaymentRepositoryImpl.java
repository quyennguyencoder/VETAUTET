package com.nguyenquyen.vetautet.ddd.infrastructure.persistence.repository;


import com.nguyenquyen.vetautet.ddd.domain.model.entity.PaymentTransaction;
import com.nguyenquyen.vetautet.ddd.domain.repository.PaymentRepository;
import com.nguyenquyen.vetautet.ddd.infrastructure.persistence.dataobject.PaymentTransactionDO;
import com.nguyenquyen.vetautet.ddd.infrastructure.persistence.mapper.PaymentJPAMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepositoryImpl implements PaymentRepository {
    @Autowired
    private PaymentJPAMapper paymentJPAMapper;
    @Override
    public boolean save(PaymentTransaction transaction) {
        PaymentTransactionDO dataObject = new PaymentTransactionDO();
        // Copy dữ liệu từ Domain Entity sang JPA Data Object
        BeanUtils.copyProperties(transaction, dataObject);
        dataObject.setUpdatedAt(java.time.LocalDateTime.now());
        dataObject.setCreatedAt(java.time.LocalDateTime.now());

        return paymentJPAMapper.save(dataObject) != null;
    }
    @Override
    public boolean updateStatus(String paymentId, Integer status, String gatewayId, String url) {
        return paymentJPAMapper.updateStatus(paymentId, status, gatewayId, url) > 0;
    }
    @Override
    public PaymentTransaction findByPaymentId(String paymentId) {
        PaymentTransactionDO dataObject = paymentJPAMapper.findByPaymentId(paymentId);
        if (dataObject == null) return null;

        PaymentTransaction entity = new PaymentTransaction();
        BeanUtils.copyProperties(dataObject, entity);
        return entity;
    }
}
