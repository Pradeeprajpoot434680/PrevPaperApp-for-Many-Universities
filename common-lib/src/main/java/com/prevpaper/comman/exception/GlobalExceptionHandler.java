package com.prevpaper.comman.exception;


import com.prevpaper.comman.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleResourceNotFound(ResourceNotFoundException exception){
        ApiResponse<String> response = ApiResponse.error(exception.getMessage());
        return new ResponseEntity<>(response,HttpStatus.NOT_FOUND);

    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<String>> handleUnauthorized(
            UnauthorizedException ex) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ResourceAlreadyExist.class)
    public ResponseEntity<ApiResponse<String>> handleResourcealreadyExist(
            ResourceAlreadyExist ex) {

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidOTP(
            InvalidOtpException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(EmptyInputBoxException.class)
    public ResponseEntity<ApiResponse<Object>> handleEmptyInputBox(
            EmptyInputBoxException ex) {

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(WrongCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> badCredentialsHandler(WrongCredentialsException ex){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusiness(
            BusinessException ex) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(RoleExceptionHandler.class)
    public ResponseEntity<ApiResponse<Object>> roleHandler(Exception ex){
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleAll(
            Exception ex) {

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Something went wrong" + ex));
    }

}

