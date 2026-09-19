/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CodeSyntaxValidateRequest } from '../models/CodeSyntaxValidateRequest';
import type { ResultCodeSyntaxValidateResponseVO } from '../models/ResultCodeSyntaxValidateResponseVO';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class CodeValidateService {
    /**
     * 按语言做语法级检查，返回行列与错误信息
     * @param requestBody
     * @returns ResultCodeSyntaxValidateResponseVO OK
     * @throws ApiError
     */
    public static syntax(
        requestBody: CodeSyntaxValidateRequest,
    ): CancelablePromise<ResultCodeSyntaxValidateResponseVO> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/code_validate/syntax',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
}
