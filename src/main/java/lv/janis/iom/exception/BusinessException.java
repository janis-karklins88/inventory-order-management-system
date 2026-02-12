package lv.janis.iom.exception;

import lv.janis.iom.enums.FailureCode;

public class BusinessException extends RuntimeException {
  private final FailureCode code;

  public BusinessException(FailureCode code, String message) {
    super(message);
    this.code = code;
  }

  public FailureCode getCode() {
    return code;
  }
}
