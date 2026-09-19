/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ContestQuestionBriefVO } from './ContestQuestionBriefVO';
export type ContestDetailVO = {
    id?: number;
    title?: string;
    description?: string;
    startTime?: string;
    endTime?: string;
    phase?: string;
    meRegistered?: boolean;
    questions?: Array<ContestQuestionBriefVO>;
};

