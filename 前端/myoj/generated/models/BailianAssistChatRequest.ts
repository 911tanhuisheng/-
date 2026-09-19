/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BailianAssistTurnDTO } from './BailianAssistTurnDTO';
export type BailianAssistChatRequest = {
    userMessage: string;
    questionId?: string;
    questionTitle?: string;
    questionContent?: string;
    language?: string;
    questionType?: string;
    judgeContext?: string;
    sampleCases?: string;
    runResult?: string;
    code?: string;
    history?: Array<BailianAssistTurnDTO>;
};

