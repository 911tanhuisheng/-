<script setup lang="ts">
import { computed } from 'vue'
import { renderMarkdownToHtml } from '@/utils/renderMarkdown'

const props = defineProps<{
  source: string
}>()

const html = computed(() => renderMarkdownToHtml(props.source ?? ''))
</script>

<template>
  <div class="blog-md" v-html="html" />
</template>

<style>
/*
  Unscoped: content comes from v-html. All selectors live under .blog-md.
  Uses inherited cyber tokens (--cb-*) when rendered inside BlogCyberShell.
*/
.blog-md {
  font-family:
    'Plus Jakarta Sans',
    'Noto Sans SC',
    'DM Sans',
    ui-sans-serif,
    system-ui,
    -apple-system,
    'Segoe UI',
    sans-serif;
  font-size: clamp(17px, 0.25vw + 16.2px, 18px);
  line-height: 1.92;
  letter-spacing: 0.01em;
  font-feature-settings: 'kern' 1, 'liga' 1;
  color: inherit;
  word-break: break-word;
  text-rendering: optimizeLegibility;
  -webkit-font-smoothing: antialiased;
}

.blog-md > :first-child {
  margin-top: 0;
}

.blog-md > :last-child {
  margin-bottom: 0;
}

.blog-md h1,
.blog-md h2,
.blog-md h3,
.blog-md h4,
.blog-md h5,
.blog-md h6 {
  font-family:
    'Noto Sans SC',
    'DM Sans',
    ui-sans-serif,
    system-ui,
    sans-serif;
  font-weight: 700;
  letter-spacing: -0.015em;
  line-height: 1.35;
  margin: 2em 0 0.75em;
  color: inherit;
}

.blog-md h1 {
  font-size: clamp(1.45rem, 2vw + 1rem, 1.75rem);
  font-weight: 800;
  padding-bottom: 0.55em;
  margin-top: 1.25em;
  border-bottom: 1px solid color-mix(in srgb, var(--cb-cyan, var(--app-accent)) 18%, transparent);
}

.blog-md h2 {
  font-size: clamp(1.22rem, 1.2vw + 1rem, 1.42rem);
  margin-top: 2.15em;
  padding-top: 0.35em;
}

.blog-md h3 {
  font-size: 1.125rem;
  font-weight: 700;
  margin-top: 1.85em;
  color: color-mix(in srgb, currentColor 94%, var(--cb-cyan, var(--app-accent)) 6%);
}

.blog-md h4 {
  font-size: 1.05rem;
  font-weight: 600;
  margin-top: 1.6em;
}

.blog-md h5,
.blog-md h6 {
  font-size: 1rem;
  font-weight: 600;
  margin-top: 1.45em;
  opacity: 0.96;
}

.blog-md p {
  margin: 1.05em 0;
}

.blog-md a {
  color: color-mix(in srgb, var(--cb-cyan, var(--app-accent)) 92%, white 8%);
  text-decoration: underline;
  text-decoration-thickness: 1px;
  text-decoration-color: color-mix(in srgb, var(--cb-cyan, var(--app-accent)) 45%, transparent);
  text-underline-offset: 5px;
  transition:
    color 0.18s ease,
    text-decoration-color 0.18s ease,
    filter 0.18s ease;
}

.blog-md a:hover {
  filter: brightness(1.08);
  text-decoration-color: color-mix(in srgb, var(--cb-cyan, var(--app-accent)) 72%, transparent);
}

.blog-md strong {
  font-weight: 700;
  color: inherit;
  letter-spacing: 0.005em;
}

.blog-md em {
  font-style: italic;
  opacity: 0.92;
}

.blog-md blockquote {
  margin: 1.65rem 0;
  padding: 1.35rem 1.5rem 1.35rem 1.55rem;
  border-left: 4px solid color-mix(in srgb, var(--cb-cyan, var(--app-accent)) 55%, transparent);
  border-radius: 6px 18px 18px 6px;
  background: color-mix(in srgb, var(--cb-cyan, var(--app-accent)) 6%, transparent);
  font-style: normal;
  font-size: 0.965em;
  line-height: 1.82;
  color: color-mix(in srgb, currentColor 78%, var(--cb-muted, var(--app-text-muted)) 22%);
}

html[data-theme='light'] .blog-md blockquote {
  background: color-mix(in srgb, var(--app-accent) 5%, transparent);
  color: color-mix(in srgb, var(--app-text) 82%, var(--app-text-muted) 18%);
}

html[data-theme='dark'] .blog-md blockquote {
  color: color-mix(in srgb, var(--cb-muted, rgba(222, 234, 252, 0.9)) 92%, rgba(255, 255, 255, 0.06) 8%);
  background: color-mix(in srgb, var(--cb-cyan, #5ee7ff) 7%, rgba(6, 10, 22, 0.65));
  border-left-color: color-mix(in srgb, var(--cb-cyan, #5ee7ff) 45%, transparent);
}

html[data-theme='dark'] .blog-md li {
  color: color-mix(in srgb, var(--cb-text, #f4f8ff) 94%, var(--cb-muted) 6%);
}

.blog-md ul,
.blog-md ol {
  margin: 1em 0;
  padding-left: 1.55em;
}

.blog-md li {
  margin: 0.45em 0;
  line-height: 1.82;
}

.blog-md li::marker {
  color: color-mix(in srgb, var(--cb-violet, var(--app-accent)) 65%, var(--cb-muted, gray) 35%);
}

.blog-md hr {
  margin: 2.65rem 0;
  border: none;
  height: 1px;
  background: linear-gradient(
    90deg,
    transparent,
    color-mix(in srgb, var(--cb-cyan, var(--app-accent)) 35%, transparent),
    transparent
  );
}

.blog-md img {
  max-width: 100%;
  height: auto;
  display: block;
  margin: 1.75rem auto;
  border-radius: 16px;
  border: 1px solid color-mix(in srgb, var(--cb-cyan, var(--app-accent)) 14%, transparent);
  box-shadow:
    0 12px 40px rgba(0, 0, 0, 0.18),
    0 0 0 1px rgba(255, 255, 255, 0.03);
}

.blog-md pre {
  margin: 1.65rem 0;
  padding: 1.25rem 1.35rem;
  overflow-x: auto;
  border-radius: 14px;
  background: rgba(8, 12, 24, 0.58);
  border: 1px solid color-mix(in srgb, var(--cb-cyan, var(--app-accent)) 12%, transparent);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.035);
}

html[data-theme='light'] .blog-md pre {
  background: rgba(15, 23, 42, 0.06);
  border-color: color-mix(in srgb, var(--app-accent) 22%, transparent);
}

.blog-md code {
  font-family: 'JetBrains Mono', ui-monospace, monospace;
  font-size: 0.88em;
  font-weight: 500;
}

.blog-md pre code {
  font-size: 0.815em;
  line-height: 1.72;
  font-weight: 400;
  color: color-mix(in srgb, var(--cb-text, var(--app-text)) 91%, var(--cb-cyan, #67e8f9) 9%);
}

html[data-theme='light'] .blog-md pre code {
  color: var(--app-text);
}

.blog-md :not(pre) > code {
  padding: 0.14em 0.48em;
  border-radius: 8px;
  background: color-mix(in srgb, var(--cb-violet, #a855f7) 9%, transparent);
  border: 1px solid color-mix(in srgb, var(--cb-violet, #a855f7) 16%, transparent);
  white-space: break-spaces;
}

.blog-md table {
  width: 100%;
  margin: 1.65rem 0;
  border-collapse: collapse;
  font-size: 0.93em;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--cb-cyan, var(--app-accent)) 11%, transparent);
}

.blog-md th,
.blog-md td {
  padding: 0.72rem 0.95rem;
  border: 1px solid color-mix(in srgb, var(--cb-cyan, var(--app-accent)) 9%, transparent);
}

.blog-md th {
  font-family: 'Noto Sans SC', 'DM Sans', sans-serif;
  font-size: 0.78rem;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  font-weight: 700;
  background: color-mix(in srgb, var(--cb-cyan, var(--app-accent)) 7%, transparent);
  color: color-mix(in srgb, var(--cb-cyan-dim, var(--app-accent)) 88%, transparent 12%);
}

.blog-md tbody tr:nth-child(even) {
  background: color-mix(in srgb, var(--cb-cyan, var(--app-accent)) 3%, transparent);
}

.blog-md input[type='checkbox'] {
  margin-right: 0.5em;
  accent-color: var(--cb-cyan, var(--app-accent));
}
</style>
