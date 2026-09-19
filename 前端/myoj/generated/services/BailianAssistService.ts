/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BailianAssistChatRequest } from '../models/BailianAssistChatRequest';
import type { SseEmitter } from '../models/SseEmitter';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class BailianAssistService {
    /**
     * 学习助手对话（SSE 流式）
     * @param requestBody
     * @returns SseEmitter OK
     * @throws ApiError
     */
    public static chat(
        requestBody: BailianAssistChatRequest,
    ): CancelablePromise<SseEmitter> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/bailian_assist/chat',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
}
