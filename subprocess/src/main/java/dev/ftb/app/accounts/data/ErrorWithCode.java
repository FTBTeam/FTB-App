package dev.ftb.app.accounts.data;

public record ErrorWithCode(
    String code, 
    String message
) {
    public ErrorWithCode extendMessageIfRequestError(String message) {
        if (CodedError.REQUEST_ERRORS.stream().anyMatch(error -> error.code.equals(code))) {
            return new ErrorWithCode(code, message + ". " + this.message);
        }
        
        return this;
    }
}
