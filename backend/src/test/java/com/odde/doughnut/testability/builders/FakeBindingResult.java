package com.odde.doughnut.testability.builders;

import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.beans.PropertyEditor;
import java.util.List;
import java.util.Map;

public class FakeBindingResult implements BindingResult {
    private final boolean hasError;

    public FakeBindingResult(boolean hasError) {

        this.hasError = hasError;
    }

    @Override
    public Object getTarget() {
        return null;
    }

    @Override
    public Map<String, Object> getModel() {
        return null;
    }

    @Override
    public Object getRawFieldValue(String field) {
        return null;
    }

    @Override
    public PropertyEditor findEditor(String field, Class<?> valueType) {
        return null;
    }

    @Override
    public PropertyEditorRegistry getPropertyEditorRegistry() {
        return null;
    }

    @Override
    public String[] resolveMessageCodes(String errorCode) {
        return new String[0];
    }

    @Override
    public String[] resolveMessageCodes(String errorCode, String field) {
        return new String[0];
    }

    @Override
    public void addError(ObjectError error) {

    }

    @Override
    public String getObjectName() {
        return null;
    }

    @Override
    public void setNestedPath(String nestedPath) {

    }

    @Override
    public String getNestedPath() {
        return null;
    }

    @Override
    public void pushNestedPath(String subPath) {

    }

    @Override
    public void popNestedPath() throws IllegalStateException {

    }

    @Override
    public void reject(String errorCode) {

    }

    @Override
    public void reject(String errorCode, String defaultMessage) {

    }

    @Override
    public void reject(String errorCode, Object[] errorArgs, String defaultMessage) {

    }

    @Override
    public void rejectValue(String field, String errorCode) {

    }

    @Override
    public void rejectValue(String field, String errorCode, String defaultMessage) {

    }

    @Override
    public void rejectValue(String field, String errorCode, Object[] errorArgs, String defaultMessage) {

    }

    @Override
    public void addAllErrors(Errors errors) {

    }

    @Override
    public boolean hasErrors() {
        return hasError;
    }

    @Override
    public int getErrorCount() {
        return 0;
    }

    @Override
    public List<ObjectError> getAllErrors() {
        return null;
    }

    @Override
    public boolean hasGlobalErrors() {
        return false;
    }

    @Override
    public int getGlobalErrorCount() {
        return 0;
    }

    @Override
    public List<ObjectError> getGlobalErrors() {
        return null;
    }

    @Override
    public ObjectError getGlobalError() {
        return null;
    }

    @Override
    public boolean hasFieldErrors() {
        return false;
    }

    @Override
    public int getFieldErrorCount() {
        return 0;
    }

    @Override
    public List<FieldError> getFieldErrors() {
        return null;
    }

    @Override
    public FieldError getFieldError() {
        return null;
    }

    @Override
    public boolean hasFieldErrors(String field) {
        return false;
    }

    @Override
    public int getFieldErrorCount(String field) {
        return 0;
    }

    @Override
    public List<FieldError> getFieldErrors(String field) {
        return null;
    }

    @Override
    public FieldError getFieldError(String field) {
        return null;
    }

    @Override
    public Object getFieldValue(String field) {
        return null;
    }

    @Override
    public Class<?> getFieldType(String field) {
        return null;
    }
}
