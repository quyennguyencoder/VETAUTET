package com.nguyenquyen.vetautet.ddd.controller.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * VO tương tác giữa frontend và backend
 *
 * @author vantrang
 */
@Data
public class ResultMessage<T> implements Serializable {

    private static final long serialVersionUID = 1L;


    private boolean success;
    private String message;
    private Integer code;
    private long timestamp = System.currentTimeMillis();
    private T result;
}
