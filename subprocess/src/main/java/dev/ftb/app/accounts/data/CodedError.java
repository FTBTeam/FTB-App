package dev.ftb.app.accounts.data;

public enum CodedError {
    // Literal request errors
    COMPLETE_REQUEST_FAILURE("auth:99", "Failed to complete request due to an error at the request level"),
    MISSING_BODY("auth:98", "Failed to read response body"),
    NOT_SUCCESSFUL("auth:97", "Response was not successful"),
    BODY_PARSE_FAILED("auth:96", "Failed to parse response body"),
    VALIDATION_FAILED("auth:95", "Response did not pass validation"),
    WRONG_BODY_TYPE("auth:94", "Response was not a JSON object"),
    BODY_READING_FAILED("auth:90", "Failed to read response body"),
    
    // Normal flow errors
    XBOX_ACCOUNT_MISSING("auth:50", "This account does not have an XBox Live account. You have likely not migrated your account."),
    XBOX_INVALID_REGION("auth:51", "Your account resides in a region that does not support Xbox Live..."),
    XBOX_ADULT_VERIFICATION("auth:52", "The account needs adult verification on Xbox page."),
    XBOX_UNDER_18("auth:53", "This is an under 18 account. You cannot proceed unless the account is added to a Family by an adult"),
    XBOX_UNKNOWN_FAILURE_CODE("auth:60", "Unknown failure code from Xbox Live"),
    
    MISSING_CLAIMS("auth:70", "Missing claims in the response"),
    MISSING_ENTITLEMENTS("auth:71", "No game ownership found for this account"),
    INVALID_PROFILE("auth:72", "Invalid profile data..."),
    ;
    
    public final String code;
    public final String message;
    
    CodedError(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public static CodedError fromCode(String code) {
        for (var error : values()) {
            if (error.code.equals(code)) {
                return error;
            }
        }
        
        return null;
    }

    public ErrorWithCode toError() {
        return new ErrorWithCode(code, message);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
