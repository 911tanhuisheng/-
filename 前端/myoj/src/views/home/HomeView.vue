<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import { LAYOUT_THEME_KEY } from '@/config/theme-injection'
import { useScrollReveal } from '@/composables/useScrollReveal'

/** 首页：首屏 + 快捷入口 + 功能说明；浅色用 .home-landing--light */
const layoutTheme = inject(LAYOUT_THEME_KEY, null)
const themeLight = computed(() =>
  layoutTheme ? layoutTheme.value === 'light' : true,
)

const quickLinks = [
  { to: '/problems', title: '题库', desc: '按难度、标签筛选', glyph: '◈', tone: 0 },
  { to: '/contests', title: '比赛', desc: '报名参赛、看榜单', glyph: '◇', tone: 1 },
  { to: '/rankings', title: '排行榜', desc: '积分与排名', glyph: '▤', tone: 2 },
  { to: '/submissions', title: '提交记录', desc: '评测结果与耗时', glyph: '▸', tone: 3 },
] as const

const featurePillars = [
  {
    tag: '题库',
    title: '按标签和难度找题',
    body: '支持筛选、题单；每道题能看到自己的 AC / WA 等状态。',
    points: ['难度、标签', '题单', '提交状态'],
    tone: 'a' as const,
  },
  {
    tag: '比赛',
    title: '限时赛与榜单',
    body: '比赛期间提交计入排名，结束后可回看榜单和题目。',
    points: ['比赛列表', '实时/赛后榜单', '积分规则'],
    tone: 'b' as const,
  },
  {
    tag: '记录',
    title: '提交与签到',
    body: '提交历史可查评测详情；签到和积分在站内展示。',
    points: ['提交记录', '签到日历', '排行榜'],
    tone: 'c' as const,
  },
] as const

const platformAdvantages = [
  {
    icon: '⚡',
    title: 'Docker 沙箱判题',
    desc: 'Java / Python / C++ 在容器里编译运行，限制时间与内存。',
  },
  {
    icon: '◆',
    title: '提交详情',
    desc: '展示 AC、WA、TLE 等状态，以及耗时、内存。',
  },
  {
    icon: '◇',
    title: '用户与管理员',
    desc: '普通用户做题、参赛；管理员维护题目和比赛。',
  },
  {
    icon: '✦',
    title: '前后端分离',
    desc: 'Vue 前端 + Spring Boot API，判题服务单独部署。',
  },
] as const

const highlightStats = [
  { num: '15+', label: '内置示例题', sub: 'schema 初始化自带' },
  { num: '3', label: '支持语言', sub: 'Java / Python / C++' },
  { num: 'SSE', label: '站内通知', sub: '提交结果推送' },
  { num: 'JWT', label: '登录态', sub: 'Redis 会话' },
] as const

const trainingSteps = [
  {
    step: '01',
    title: '选题',
    body: '在题库按难度或标签找题，也可以跟题单练。',
    hint: '题库',
  },
  {
    step: '02',
    title: '提交',
    body: '在线编辑器写代码，样例运行或正式提交，等评测结果。',
    hint: '做题页',
  },
  {
    step: '03',
    title: '比赛',
    body: '报名参加限时赛，在榜单里看自己和别人的排名。',
    hint: '比赛',
  },
] as const

const judgeSteps = [
  { title: '接收提交', body: '保存代码、语言和时间。' },
  { title: '编译', body: '在沙箱里编译，受 CPU/内存限制。' },
  { title: '跑测例', body: '逐个测例运行，比对输出。' },
  { title: '回写结果', body: '更新 AC/WA/TLE 等状态和耗时。' },
] as const

const useScenarios = [
  {
    icon: '🎓',
    title: '课程作业',
    desc: '老师出题、学生在线提交，平台负责判题和统计。',
  },
  {
    icon: '⛺',
    title: '校赛 / 集训',
    desc: '按比赛时间开放题目，结束后看榜单。',
  },
  {
    icon: '🧑‍💻',
    title: '个人刷题',
    desc: '平时练题、看提交记录和排行榜。',
  },
  {
    icon: '🏢',
    title: '机试练习',
    desc: '限时做题，提交记录可回看。',
  },
] as const

const landingRef = ref<HTMLElement | null>(null)
useScrollReveal(landingRef)
</script>

<template>
  <div ref="landingRef" class="home-landing" :class="{ 'home-landing--light': themeLight }">
    <!-- —— 全宽深色首屏（OJ 站台感） —— -->
    <section class="oj-hero">
      <div class="oj-hero-bg" aria-hidden="true">
        <div class="oj-mesh" />
        <div class="oj-grid" />
        <div class="oj-glow oj-glow--a" />
        <div class="oj-glow oj-glow--b" />
        <div class="oj-stars" aria-hidden="true">
          <div class="oj-stars-aurora" />
          <div class="oj-stars-layer oj-stars-layer--far" />
          <div class="oj-stars-layer oj-stars-layer--mid" />
          <div class="oj-stars-layer oj-stars-layer--near" />
          <div class="oj-stars-meteor oj-stars-meteor--a" />
          <div class="oj-stars-meteor oj-stars-meteor--b" />
          <div class="oj-stars-meteor oj-stars-meteor--c" />
        </div>
        <div class="oj-beam" />
      </div>

      <div class="oj-hero-inner">
        <div class="oj-hero-left">
          <p class="oj-badge hero-in hero-in--1">
            <span class="oj-badge-dot" />
            在线评测 · 算法练习 · 自动判题
          </p>
          <p class="oj-slogan oj-slogan--shine hero-in hero-in--3">
            <span class="oj-slogan__text">在线做题，</span><span class="oj-slogan__accent"
              >自动判题。</span
            >
          </p>
          <p class="oj-lead oj-lead--rich hero-in hero-in--4">
            题库、比赛、提交记录和排行榜都在这一个站点里，<span class="oj-lead__gradient"
              >适合平时刷题和校内比赛</span
            >。
          </p>

          <div class="oj-cta hero-in hero-in--5">
            <RouterLink to="/problems" class="oj-btn oj-btn--primary">开始做题</RouterLink>
            <RouterLink to="/contests" class="oj-btn oj-btn--ghost">进入比赛</RouterLink>
            <RouterLink to="/rankings" class="oj-btn oj-btn--ghost">查看榜单</RouterLink>
          </div>

          <div class="oj-stats hero-in hero-in--6">
            <div class="oj-stat">
              <strong>评测</strong>
              <span>自动判题 · 多语言</span>
            </div>
            <div class="oj-stat">
              <strong>训练</strong>
              <span>专题 · 题单 · 进度</span>
            </div>
            <div class="oj-stat">
              <strong>竞技</strong>
              <span>榜单 · 积分 · 签到</span>
            </div>
          </div>
        </div>

        <div class="oj-hero-right hero-in hero-in--5">
          <div class="oj-code-cluster">
            <div class="oj-terminal oj-terminal--feature">
              <div class="oj-terminal-bar">
                <span class="oj-dots" aria-hidden="true" />
                <span class="oj-terminal-name">main.cpp</span>
              </div>
              <div class="oj-terminal-mid">
                <aside class="oj-float-rail" aria-label="训练状态">
                  <div class="oj-strip-card oj-strip-card--float oj-strip-card--a">
                    <div class="oj-strip-card__head">
                      <span class="oj-strip-card__k">今日签到</span>
                      <span class="oj-strip-card__ico" aria-hidden="true">✓</span>
                    </div>
                    <strong class="oj-strip-card__v">+100</strong>
                    <span class="oj-strip-card__hint">较昨日 +20%</span>
                    <span class="oj-strip-card__line" aria-hidden="true" />
                  </div>
                  <div class="oj-strip-card oj-strip-card--float oj-strip-card--b">
                    <div class="oj-strip-card__head">
                      <span class="oj-strip-card__k">连续训练</span>
                      <span class="oj-strip-card__ico" aria-hidden="true">↗</span>
                    </div>
                    <strong class="oj-strip-card__v">4 天</strong>
                    <span class="oj-strip-card__hint">本周目标 7 天</span>
                    <div
                      class="oj-strip-card__meter"
                      role="progressbar"
                      aria-label="连续训练进度"
                      aria-valuemin="0"
                      aria-valuemax="7"
                      aria-valuenow="4"
                    >
                      <span class="oj-strip-card__meter-fill" />
                    </div>
                  </div>
                </aside>
                <div class="oj-terminal-code-col">
                  <pre class="oj-code" tabindex="-1"><code><span class="tok-k">#include</span> <span class="tok-s">&lt;bits/stdc++.h&gt;</span>
<span class="tok-k">using namespace</span> <span class="tok-t">std</span><span class="tok-p">;</span>

<span class="tok-k">int</span> <span class="tok-f">main</span><span class="tok-p">() {</span>
  <span class="tok-t">ios</span><span class="tok-p">::</span><span class="tok-f">sync_with_stdio</span><span class="tok-p">(</span><span class="tok-n">false</span><span class="tok-p">);</span>
  <span class="tok-t">cin</span><span class="tok-p">.</span><span class="tok-f">tie</span><span class="tok-p">(</span><span class="tok-k">nullptr</span><span class="tok-p">);</span>
  <span class="tok-k">int</span> n<span class="tok-p">;</span> <span class="tok-t">cin</span> <span class="tok-p">&gt;&gt;</span> n<span class="tok-p">;</span>
  <span class="tok-c">// 今日目标：稳定输出 AC</span>
  <span class="tok-t">cout</span> <span class="tok-p">&lt;&lt;</span> n <span class="tok-p">&lt;&lt;</span> <span class="tok-s">"\n"</span><span class="tok-p">;</span>
  <span class="tok-k">return</span> <span class="tok-m">0</span><span class="tok-p">;</span>
<span class="tok-p">}</span></code></pre>
                  <div class="oj-run">
                    <span class="oj-ac">✓ Accepted</span>
                    <span class="oj-meta">92 ms · 8.1 MB</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="oj-code-minis">
              <div class="oj-mini-terminal">
                <div class="oj-terminal-bar oj-terminal-bar--mini">
                  <span class="oj-dots" aria-hidden="true" />
                  <span class="oj-terminal-name">run.sh</span>
                </div>
                <pre class="oj-code oj-code--mini" tabindex="-1"><code><span class="tok-c">#!/usr/bin/env bash</span>
<span class="tok-f">g++</span> -std=c++20 -O2 main.c -o a.out
<span class="tok-f">./a.out</span> <span class="tok-p">&lt;</span> sample.in</code></pre>
              </div>
              <div class="oj-mini-terminal oj-mini-terminal--alt">
                <div class="oj-terminal-bar oj-terminal-bar--mini">
                  <span class="oj-dots" aria-hidden="true" />
                  <span class="oj-terminal-name">problem.yaml</span>
                </div>
                <pre class="oj-code oj-code--mini" tabindex="-1"><code><span class="tok-t">time_limit_ms</span><span class="tok-p">:</span> <span class="tok-m">1000</span>
<span class="tok-t">memory_mb</span><span class="tok-p">:</span> <span class="tok-m">256</span>
<span class="tok-t">checker</span><span class="tok-p">:</span> <span class="tok-s">"ncmp"</span></code></pre>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 主体：与全站主题一致 -->
    <div class="home-body">
      <header class="home-section-head" data-scroll-reveal>
        <p class="home-section-eyebrow">功能入口</p>
        <h2 class="home-section-title">常用入口</h2>
        <p class="home-section-lead">做题、参赛、看排名和提交记录。</p>
      </header>

      <section class="oj-quick">
        <RouterLink
          v-for="(item, idx) in quickLinks"
          :key="item.to"
          :to="item.to"
          class="oj-quick-card"
          :class="`oj-quick-card--t${item.tone}`"
          data-scroll-reveal
          :style="{ '--sr-delay': `${idx * 0.07}s` }"
        >
          <span class="oj-quick-card__aurora" aria-hidden="true" />
          <span class="oj-quick-card__edge" aria-hidden="true" />
          <span class="oj-quick-glyph" aria-hidden="true">{{ item.glyph }}</span>
          <strong>{{ item.title }}</strong>
          <span>{{ item.desc }}</span>
        </RouterLink>
      </section>

      <section class="oj-highlight-strip" aria-label="平台亮点数据">
        <div
          v-for="(s, idx) in highlightStats"
          :key="s.label"
          class="oj-highlight-card panel"
          data-scroll-reveal
          :style="{ '--sr-delay': `${idx * 0.08}s` }"
        >
          <strong class="oj-highlight-card__num">{{ s.num }}</strong>
          <span class="oj-highlight-card__label">{{ s.label }}</span>
          <span class="oj-highlight-card__sub">{{ s.sub }}</span>
        </div>
      </section>

      <header class="home-section-head home-section-head--tight" data-scroll-reveal>
        <p class="home-section-eyebrow">核心特性</p>
        <h2 class="home-section-title">主要功能</h2>
      </header>

      <section class="feature-grid">
        <article
          v-for="(p, idx) in featurePillars"
          :key="p.title"
          class="oj-pillar panel"
          :class="`oj-pillar--${p.tone}`"
          data-scroll-reveal
          :style="{ '--sr-delay': `${idx * 0.1}s` }"
        >
          <div class="oj-pillar__bg" aria-hidden="true" />
          <p class="oj-pillar__tag">{{ p.tag }}</p>
          <h3>{{ p.title }}</h3>
          <p class="oj-pillar__body">{{ p.body }}</p>
          <ul class="oj-pillar__points">
            <li v-for="pt in p.points" :key="pt">{{ pt }}</li>
          </ul>
        </article>
      </section>

      <section class="oj-advantages panel">
        <div class="oj-advantages__bg" aria-hidden="true" />
        <div class="oj-advantages__inner">
          <div class="oj-advantages__intro" data-scroll-reveal>
            <p class="home-section-eyebrow">平台优势</p>
            <h2 class="oj-advantages__title">技术说明</h2>
            <p class="oj-advantages__lead">
              判题走独立 Sandbox 服务，主站负责用户、题目和比赛等业务。
            </p>
          </div>
          <div class="oj-advantages__grid">
            <div
              v-for="(adv, idx) in platformAdvantages"
              :key="adv.title"
              class="oj-adv-card"
              data-scroll-reveal
              :style="{ '--sr-delay': `${idx * 0.09}s` }"
            >
              <span class="oj-adv-card__icon" aria-hidden="true">{{ adv.icon }}</span>
              <div class="oj-adv-card__text">
                <strong>{{ adv.title }}</strong>
                <span>{{ adv.desc }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <header class="home-section-head home-section-head--tight" data-scroll-reveal>
        <p class="home-section-eyebrow">训练路径</p>
        <h2 class="home-section-title">怎么用</h2>
        <p class="home-section-lead">选题 → 提交 → 参加比赛，三步就够。</p>
      </header>

      <section class="oj-path" aria-label="训练路径">
        <article
          v-for="(st, idx) in trainingSteps"
          :key="st.step"
          class="oj-path-step panel"
          data-scroll-reveal
          :style="{ '--sr-delay': `${idx * 0.11}s` }"
        >
          <span class="oj-path-step__n">{{ st.step }}</span>
          <h3>{{ st.title }}</h3>
          <p>{{ st.body }}</p>
          <span class="oj-path-step__hint">{{ st.hint }}</span>
        </article>
      </section>

      <header class="home-section-head home-section-head--tight" data-scroll-reveal>
        <p class="home-section-eyebrow">判题流水线</p>
        <h2 class="home-section-title">每一次提交，后台在做什么</h2>
      </header>

      <section class="oj-pipeline panel" aria-label="判题流程">
        <div class="oj-pipeline__row">
          <template v-for="(js, idx) in judgeSteps" :key="js.title">
            <div
              class="oj-pipeline__step"
              data-scroll-reveal
              :style="{ '--sr-delay': `${idx * 0.08}s` }"
            >
              <span class="oj-pipeline__dot" aria-hidden="true" />
              <div class="oj-pipeline__body">
                <strong>{{ js.title }}</strong>
                <span>{{ js.body }}</span>
              </div>
            </div>
            <span
              v-if="idx < judgeSteps.length - 1"
              class="oj-pipeline__arrow"
              aria-hidden="true"
            >→</span>
          </template>
        </div>
      </section>

      <header class="home-section-head home-section-head--tight" data-scroll-reveal>
        <p class="home-section-eyebrow">适用场景</p>
        <h2 class="home-section-title">适用场景</h2>
      </header>

      <section class="oj-scenarios">
        <article
          v-for="(sc, idx) in useScenarios"
          :key="sc.title"
          class="oj-scenario panel"
          data-scroll-reveal
          :style="{ '--sr-delay': `${idx * 0.09}s` }"
        >
          <span class="oj-scenario__icon" aria-hidden="true">{{ sc.icon }}</span>
          <h3>{{ sc.title }}</h3>
          <p>{{ sc.desc }}</p>
        </article>
      </section>

    </div>
  </div>
</template>

<style scoped>
.home-landing {
  position: relative;
  z-index: 1;
  width: 100%;
  overflow-x: visible;
}

/* ========== 赛氪式全宽首屏（独立色板，不依赖全局主题） ========== */
.oj-hero {
  --oj-ink: #030711;
  --oj-panel: rgba(12, 22, 38, 0.72);
  --oj-line: rgba(120, 190, 255, 0.12);
  --oj-cyan: #5eead4;
  --oj-blue: #38bdf8;
  --oj-amber: #fbbf24;
  --oj-text: #e2e8f0;
  --oj-muted: #94a3b8;

  position: relative;
  width: 100vw;
  max-width: 100%;
  margin-left: calc(50% - 50vw);
  min-height: min(720px, calc(100dvh - 56px));
  padding: 48px 20px 56px;
  color: var(--oj-text);
  border-bottom: 1px solid rgba(56, 189, 248, 0.12);
}

.oj-hero-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background: linear-gradient(168deg, var(--oj-ink) 0%, #0a1628 42%, #0c1e35 100%);
}

.oj-mesh {
  position: absolute;
  inset: -40%;
  background: conic-gradient(
    from 210deg at 50% 50%,
    rgba(56, 189, 248, 0.07),
    transparent 25%,
    rgba(94, 234, 212, 0.06),
    transparent 50%,
    rgba(59, 130, 246, 0.05),
    transparent 75%,
    rgba(56, 189, 248, 0.07)
  );
  animation: ojMesh 100s linear infinite;
  opacity: 0.9;
}

.oj-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(148, 163, 184, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.06) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse 85% 70% at 50% 20%, black, transparent 72%);
}

.oj-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  pointer-events: none;
  animation: ojGlow 36s ease-in-out infinite;
}

.oj-glow--a {
  width: min(70vw, 560px);
  height: min(70vw, 560px);
  top: -12%;
  left: 20%;
  background: radial-gradient(circle, rgba(56, 189, 248, 0.25) 0%, transparent 70%);
}

.oj-glow--b {
  width: min(55vw, 480px);
  height: min(55vw, 480px);
  bottom: -8%;
  right: 5%;
  background: radial-gradient(circle, rgba(45, 212, 191, 0.2) 0%, transparent 68%);
  animation-delay: -14s;
  animation-direction: reverse;
}

/* —— 首屏星野：极光层 + 三层视差星点 + 流星（纯 CSS） —— */
.oj-stars {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.oj-stars-aurora {
  position: absolute;
  inset: -25%;
  opacity: 0.42;
  background:
    radial-gradient(ellipse 90% 55% at 18% 28%, rgba(56, 189, 248, 0.22) 0%, transparent 55%),
    radial-gradient(ellipse 70% 50% at 82% 18%, rgba(139, 92, 246, 0.18) 0%, transparent 52%),
    radial-gradient(ellipse 65% 45% at 55% 88%, rgba(45, 212, 191, 0.14) 0%, transparent 50%),
    radial-gradient(ellipse 50% 40% at 72% 48%, rgba(236, 72, 153, 0.08) 0%, transparent 48%);
  filter: blur(48px);
  animation: ojAurora 26s ease-in-out infinite alternate;
}

.oj-stars-layer {
  position: absolute;
  inset: 0;
  background-repeat: repeat;
  will-change: transform, opacity;
}

/* 远景：密而细，慢速漂移 */
.oj-stars-layer--far {
  background-image:
    radial-gradient(1px 1px at 8% 12%, rgba(255, 255, 255, 0.42), transparent),
    radial-gradient(1px 1px at 18% 44%, rgba(255, 255, 255, 0.28), transparent),
    radial-gradient(1px 1px at 31% 78%, rgba(255, 255, 255, 0.35), transparent),
    radial-gradient(1px 1px at 44% 23%, rgba(255, 255, 255, 0.22), transparent),
    radial-gradient(1px 1px at 52% 91%, rgba(255, 255, 255, 0.3), transparent),
    radial-gradient(1px 1px at 63% 38%, rgba(255, 255, 255, 0.26), transparent),
    radial-gradient(1px 1px at 71% 12%, rgba(255, 255, 255, 0.38), transparent),
    radial-gradient(1px 1px at 84% 56%, rgba(255, 255, 255, 0.24), transparent),
    radial-gradient(1px 1px at 92% 29%, rgba(255, 255, 255, 0.32), transparent),
    radial-gradient(1px 1px at 11% 67%, rgba(255, 255, 255, 0.2), transparent),
    radial-gradient(1px 1px at 26% 19%, rgba(255, 255, 255, 0.34), transparent),
    radial-gradient(1px 1px at 38% 52%, rgba(255, 255, 255, 0.27), transparent),
    radial-gradient(1px 1px at 47% 6%, rgba(255, 255, 255, 0.3), transparent),
    radial-gradient(1px 1px at 59% 64%, rgba(255, 255, 255, 0.25), transparent),
    radial-gradient(1px 1px at 68% 41%, rgba(255, 255, 255, 0.33), transparent),
    radial-gradient(1px 1px at 76% 83%, rgba(255, 255, 255, 0.21), transparent),
    radial-gradient(1px 1px at 88% 14%, rgba(255, 255, 255, 0.36), transparent),
    radial-gradient(1px 1px at 95% 72%, rgba(255, 255, 255, 0.28), transparent);
  background-size: 100% 100%;
  opacity: 0.55;
  animation: ojStarDriftFar 95s linear infinite, ojStarTwinkleA 7s ease-in-out infinite alternate;
}

/* 中景：稍大、闪烁节奏不同 */
.oj-stars-layer--mid {
  background-image:
    radial-gradient(1.6px 1.6px at 14% 28%, rgba(186, 230, 253, 0.55), transparent),
    radial-gradient(1.4px 1.4px at 28% 71%, rgba(255, 255, 255, 0.45), transparent),
    radial-gradient(1.5px 1.5px at 41% 16%, rgba(165, 243, 252, 0.5), transparent),
    radial-gradient(1.3px 1.3px at 54% 54%, rgba(255, 255, 255, 0.4), transparent),
    radial-gradient(1.6px 1.6px at 67% 88%, rgba(224, 242, 254, 0.48), transparent),
    radial-gradient(1.4px 1.4px at 79% 35%, rgba(255, 255, 255, 0.42), transparent),
    radial-gradient(1.5px 1.5px at 88% 62%, rgba(186, 230, 253, 0.52), transparent),
    radial-gradient(1.3px 1.3px at 7% 91%, rgba(255, 255, 255, 0.38), transparent),
    radial-gradient(1.6px 1.6px at 22% 48%, rgba(255, 255, 255, 0.46), transparent),
    radial-gradient(1.4px 1.4px at 36% 8%, rgba(207, 250, 254, 0.44), transparent),
    radial-gradient(1.5px 1.5px at 49% 76%, rgba(255, 255, 255, 0.4), transparent),
    radial-gradient(1.3px 1.3px at 61% 22%, rgba(186, 230, 253, 0.5), transparent),
    radial-gradient(1.6px 1.6px at 73% 95%, rgba(255, 255, 255, 0.36), transparent),
    radial-gradient(1.4px 1.4px at 91% 44%, rgba(224, 242, 254, 0.48), transparent);
  background-size: 100% 100%;
  opacity: 0.62;
  animation: ojStarDriftMid 68s linear infinite reverse, ojStarTwinkleB 5.5s ease-in-out infinite alternate;
  animation-delay: 0s, -1.2s;
}

/* 近景：大亮点 + 青辉 */
.oj-stars-layer--near {
  background-image:
    radial-gradient(2.2px 2.2px at 12% 38%, rgba(255, 255, 255, 0.85), transparent),
    radial-gradient(2px 2px at 25% 12%, rgba(56, 189, 248, 0.55), transparent),
    radial-gradient(2.4px 2.4px at 38% 72%, rgba(255, 255, 255, 0.75), transparent),
    radial-gradient(2px 2px at 51% 28%, rgba(94, 234, 212, 0.45), transparent),
    radial-gradient(2.2px 2.2px at 64% 58%, rgba(255, 255, 255, 0.8), transparent),
    radial-gradient(2px 2px at 76% 18%, rgba(186, 230, 253, 0.65), transparent),
    radial-gradient(2.3px 2.3px at 87% 82%, rgba(255, 255, 255, 0.7), transparent),
    radial-gradient(2px 2px at 6% 58%, rgba(56, 189, 248, 0.5), transparent),
    radial-gradient(2.2px 2.2px at 19% 88%, rgba(255, 255, 255, 0.78), transparent),
    radial-gradient(2px 2px at 33% 44%, rgba(165, 243, 252, 0.55), transparent),
    radial-gradient(2.4px 2.4px at 46% 8%, rgba(255, 255, 255, 0.82), transparent),
    radial-gradient(2px 2px at 58% 92%, rgba(94, 234, 212, 0.48), transparent),
    radial-gradient(2.2px 2.2px at 71% 36%, rgba(255, 255, 255, 0.72), transparent),
    radial-gradient(2px 2px at 83% 64%, rgba(224, 242, 254, 0.6), transparent),
    radial-gradient(2.3px 2.3px at 94% 22%, rgba(255, 255, 255, 0.8), transparent);
  background-size: 100% 100%;
  opacity: 0.72;
  animation: ojStarDriftNear 48s linear infinite, ojStarTwinkleC 4s ease-in-out infinite alternate;
  animation-delay: 0s, -2.4s;
}

.oj-stars-meteor {
  --meteor-rot: -36deg;
  position: absolute;
  width: min(140px, 28vw);
  height: 1px;
  border-radius: 1px;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.05) 20%,
    rgba(186, 230, 253, 0.95) 55%,
    rgba(255, 255, 255, 0.9) 72%,
    transparent 100%
  );
  box-shadow: 0 0 12px 1px rgba(56, 189, 248, 0.35);
  opacity: 0;
  transform-origin: left center;
}

.oj-stars-meteor--a {
  top: 18%;
  left: 78%;
  --meteor-rot: -38deg;
  animation: ojMeteor 8.5s ease-in-out infinite;
  animation-delay: 0s;
}

.oj-stars-meteor--b {
  top: 42%;
  left: 12%;
  width: min(100px, 22vw);
  --meteor-rot: -32deg;
  animation: ojMeteor 11s ease-in-out infinite;
  animation-delay: -3.2s;
}

.oj-stars-meteor--c {
  top: 8%;
  left: 38%;
  width: min(120px, 24vw);
  --meteor-rot: -41deg;
  animation: ojMeteor 9.8s ease-in-out infinite;
  animation-delay: -6.5s;
}

.oj-beam {
  position: absolute;
  top: -20%;
  left: 55%;
  width: 38%;
  height: 120%;
  background: linear-gradient(
    105deg,
    transparent 40%,
    rgba(56, 189, 248, 0.04) 50%,
    rgba(94, 234, 212, 0.06) 52%,
    transparent 62%
  );
  transform: skewX(-12deg);
  animation: ojBeam 16s ease-in-out infinite;
  pointer-events: none;
}

/*
 * 浅色首屏：避免大模糊 + 多层半透明叠成「白雾」。
 * 用纸色径向高光 + 略强网格结构 + 小范围柔光（blur 收敛）。
 */
.home-landing--light .oj-hero {
  color: #1c1917;
  border-bottom-color: rgba(15, 118, 110, 0.18);
  /* 去掉大块内阴影，减轻与浅色背景糊成一片的「白屏感」 */
  box-shadow: none;
}

.home-landing--light .oj-hero-bg {
  background:
    radial-gradient(ellipse 75% 48% at 82% 0%, rgba(255, 130, 70, 0.22) 0%, transparent 58%),
    radial-gradient(ellipse 55% 42% at 8% 100%, rgba(15, 118, 110, 0.14) 0%, transparent 52%),
    radial-gradient(ellipse 50% 35% at 50% 120%, rgba(180, 140, 95, 0.08) 0%, transparent 45%),
    linear-gradient(168deg, #ebe0d2 0%, #e3d5c4 38%, #d8c9b6 100%);
}

.home-landing--light .oj-mesh {
  opacity: 0.18;
  animation: none;
  background: conic-gradient(
    from 200deg at 62% 35%,
    rgba(15, 118, 110, 0.12),
    transparent 22%,
    rgba(220, 110, 55, 0.08),
    transparent 48%,
    rgba(15, 118, 110, 0.06),
    transparent 72%,
    rgba(220, 110, 55, 0.06)
  );
}

.home-landing--light .oj-grid {
  background-size: 36px 36px;
  background-image:
    linear-gradient(rgba(92, 68, 46, 0.11) 1px, transparent 1px),
    linear-gradient(90deg, rgba(92, 68, 46, 0.11) 1px, transparent 1px);
  mask-image: radial-gradient(ellipse 92% 75% at 50% 22%, black 0%, transparent 78%);
  opacity: 1;
}

/* 浅色下关闭 blur，避免与 color-scheme / 主题切换叠加时部分浏览器整页白屏 */
.home-landing--light .oj-glow {
  filter: none;
  opacity: 0.55;
}

.home-landing--light .oj-glow--a {
  width: min(58vw, 480px);
  height: min(48vw, 400px);
  background: radial-gradient(
    circle at 45% 40%,
    rgba(255, 120, 55, 0.42) 0%,
    rgba(255, 170, 110, 0.12) 42%,
    transparent 68%
  );
}

.home-landing--light .oj-glow--b {
  width: min(50vw, 420px);
  height: min(44vw, 380px);
  background: radial-gradient(
    circle at 55% 55%,
    rgba(13, 115, 105, 0.32) 0%,
    rgba(45, 160, 140, 0.1) 48%,
    transparent 65%
  );
}

/* 浅色：无大 blur，暖色微粒 + 极淡流光，避免糊成白块 */
.home-landing--light .oj-stars-aurora {
  filter: none;
  opacity: 0.28;
  inset: -15%;
  background:
    radial-gradient(ellipse 85% 50% at 22% 32%, rgba(255, 140, 80, 0.18) 0%, transparent 55%),
    radial-gradient(ellipse 70% 48% at 78% 22%, rgba(13, 148, 136, 0.14) 0%, transparent 52%),
    radial-gradient(ellipse 55% 40% at 50% 85%, rgba(180, 140, 95, 0.1) 0%, transparent 50%);
  animation: ojAuroraLight 32s ease-in-out infinite alternate;
}

.home-landing--light .oj-stars-layer--far {
  opacity: 0.35;
  background-image:
    radial-gradient(1px 1px at 10% 15%, rgba(120, 80, 45, 0.35), transparent),
    radial-gradient(1px 1px at 24% 48%, rgba(13, 115, 105, 0.28), transparent),
    radial-gradient(1px 1px at 38% 22%, rgba(180, 120, 60, 0.3), transparent),
    radial-gradient(1px 1px at 51% 76%, rgba(92, 68, 46, 0.25), transparent),
    radial-gradient(1px 1px at 64% 34%, rgba(13, 148, 136, 0.22), transparent),
    radial-gradient(1px 1px at 77% 88%, rgba(160, 100, 50, 0.28), transparent),
    radial-gradient(1px 1px at 88% 18%, rgba(80, 120, 110, 0.26), transparent),
    radial-gradient(1px 1px at 15% 62%, rgba(120, 80, 45, 0.22), transparent),
    radial-gradient(1px 1px at 33% 91%, rgba(13, 115, 105, 0.24), transparent),
    radial-gradient(1px 1px at 46% 8%, rgba(180, 120, 60, 0.26), transparent),
    radial-gradient(1px 1px at 59% 52%, rgba(92, 68, 46, 0.2), transparent),
    radial-gradient(1px 1px at 72% 14%, rgba(13, 148, 136, 0.2), transparent),
    radial-gradient(1px 1px at 85% 64%, rgba(160, 100, 50, 0.24), transparent),
    radial-gradient(1px 1px at 6% 38%, rgba(80, 120, 110, 0.22), transparent);
  animation: ojStarDriftFar 120s linear infinite, ojStarTwinkleA 9s ease-in-out infinite alternate;
}

.home-landing--light .oj-stars-layer--mid {
  opacity: 0.42;
  background-image:
    radial-gradient(1.6px 1.6px at 16% 30%, rgba(13, 115, 105, 0.45), transparent),
    radial-gradient(1.4px 1.4px at 30% 68%, rgba(180, 110, 55, 0.38), transparent),
    radial-gradient(1.5px 1.5px at 44% 20%, rgba(80, 120, 110, 0.4), transparent),
    radial-gradient(1.3px 1.3px at 56% 58%, rgba(120, 80, 45, 0.35), transparent),
    radial-gradient(1.6px 1.6px at 69% 12%, rgba(13, 148, 136, 0.42), transparent),
    radial-gradient(1.4px 1.4px at 81% 78%, rgba(180, 120, 60, 0.36), transparent),
    radial-gradient(1.5px 1.5px at 9% 84%, rgba(92, 68, 46, 0.32), transparent),
    radial-gradient(1.3px 1.3px at 23% 44%, rgba(13, 115, 105, 0.38), transparent),
    radial-gradient(1.6px 1.6px at 37% 92%, rgba(160, 100, 50, 0.34), transparent),
    radial-gradient(1.4px 1.4px at 50% 6%, rgba(80, 120, 110, 0.36), transparent),
    radial-gradient(1.5px 1.5px at 62% 48%, rgba(13, 148, 136, 0.34), transparent),
    radial-gradient(1.3px 1.3px at 75% 26%, rgba(120, 80, 45, 0.3), transparent),
    radial-gradient(1.6px 1.6px at 90% 56%, rgba(180, 110, 55, 0.38), transparent);
  animation: ojStarDriftMid 85s linear infinite reverse, ojStarTwinkleB 6.5s ease-in-out infinite alternate;
}

.home-landing--light .oj-stars-layer--near {
  opacity: 0.5;
  background-image:
    radial-gradient(2.2px 2.2px at 14% 40%, rgba(255, 250, 244, 0.9), transparent),
    radial-gradient(2px 2px at 27% 16%, rgba(13, 148, 136, 0.55), transparent),
    radial-gradient(2.4px 2.4px at 40% 74%, rgba(251, 191, 36, 0.45), transparent),
    radial-gradient(2px 2px at 53% 30%, rgba(255, 255, 255, 0.75), transparent),
    radial-gradient(2.2px 2.2px at 66% 60%, rgba(13, 115, 105, 0.5), transparent),
    radial-gradient(2px 2px at 78% 22%, rgba(255, 200, 120, 0.42), transparent),
    radial-gradient(2.3px 2.3px at 89% 86%, rgba(255, 250, 244, 0.85), transparent),
    radial-gradient(2px 2px at 8% 52%, rgba(13, 148, 136, 0.48), transparent),
    radial-gradient(2.2px 2.2px at 21% 8%, rgba(180, 120, 60, 0.4), transparent),
    radial-gradient(2px 2px at 34% 94%, rgba(255, 255, 255, 0.7), transparent),
    radial-gradient(2.4px 2.4px at 48% 42%, rgba(13, 115, 105, 0.46), transparent),
    radial-gradient(2px 2px at 60% 10%, rgba(251, 191, 36, 0.38), transparent),
    radial-gradient(2.2px 2.2px at 73% 70%, rgba(255, 250, 244, 0.8), transparent),
    radial-gradient(2px 2px at 85% 38%, rgba(80, 120, 110, 0.44), transparent);
  animation: ojStarDriftNear 58s linear infinite, ojStarTwinkleC 5s ease-in-out infinite alternate;
}

.home-landing--light .oj-stars-meteor {
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(255, 200, 120, 0.15) 25%,
    rgba(13, 148, 136, 0.75) 58%,
    rgba(255, 250, 244, 0.85) 75%,
    transparent 100%
  );
  box-shadow: 0 0 10px 1px rgba(13, 148, 136, 0.25);
}

.home-landing--light .oj-stars-meteor--a {
  animation-duration: 10.5s;
}

.home-landing--light .oj-stars-meteor--b {
  animation-duration: 13s;
}

.home-landing--light .oj-stars-meteor--c {
  animation-duration: 11.8s;
}

.home-landing--light .oj-beam {
  opacity: 0;
  animation: none;
}

.home-landing--light .oj-badge {
  color: #4a3b2f;
  background: rgba(255, 250, 244, 0.95);
  border-color: rgba(92, 68, 46, 0.18);
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.8);
}

.home-landing--light .oj-badge-dot {
  background: #0d9488;
  box-shadow: 0 0 10px rgba(13, 148, 136, 0.45);
}

.home-landing--light .oj-brand {
  color: #0d5c52;
  background: none;
  animation: none;
}

.home-landing--light .oj-slogan--shine {
  filter: none;
}

.home-landing--light .oj-slogan__text {
  background-image: linear-gradient(118deg, #1c1917 8%, #44403c 45%, #292524 92%);
}

.home-landing--light .oj-slogan__accent {
  background-image: linear-gradient(
    105deg,
    #0f766e 0%,
    #0d9488 28%,
    #059669 52%,
    #ca8a04 88%
  );
}

.home-landing--light .oj-lead--rich {
  color: #57534e;
}

.home-landing--light .oj-lead__gradient {
  background-image: linear-gradient(92deg, #0f766e 0%, #0e7490 42%, #b45309 100%);
}

.home-landing--light .oj-btn--primary {
  box-shadow: 0 12px 28px rgba(15, 140, 122, 0.22);
}

.home-landing--light .oj-btn--ghost {
  color: #1c1917;
  border-color: rgba(92, 68, 46, 0.28);
  background: rgba(255, 250, 244, 0.88);
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.75);
}

.home-landing--light .oj-btn--ghost:hover {
  border-color: rgba(13, 148, 136, 0.5);
  background: #fffefb;
}

.home-landing--light .oj-stats {
  border-top-color: rgba(92, 68, 46, 0.14);
}

.home-landing--light .oj-stat strong {
  background-image: linear-gradient(92deg, #134e4a 0%, #0f766e 42%, #1e293b 95%);
  background-size: 150% auto;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.home-landing--light .oj-stat span {
  color: #64748b;
}

.home-landing--light .oj-float-rail {
  background:
    linear-gradient(180deg, rgba(255, 250, 244, 0.76), rgba(255, 252, 248, 0.72)),
    radial-gradient(ellipse at 20% 8%, rgba(15, 118, 110, 0.12), transparent 56%);
  border-color: rgba(92, 68, 46, 0.12);
}

.home-landing--light .oj-strip-card {
  background: rgba(255, 252, 248, 0.96);
  border-color: rgba(15, 118, 110, 0.22);
  color: #57534e;
  box-shadow:
    0 10px 22px rgba(62, 45, 28, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.88);
}

.home-landing--light .oj-strip-card__k {
  color: #78716c;
}

.home-landing--light .oj-strip-card__ico {
  color: #0f766e;
  border-color: rgba(15, 118, 110, 0.26);
  background: rgba(240, 253, 250, 0.86);
}

.home-landing--light .oj-strip-card__v {
  color: #b45309;
}

.home-landing--light .oj-strip-card__hint {
  color: #78716c;
}

.home-landing--light .oj-strip-card__line {
  background: linear-gradient(90deg, rgba(15, 118, 110, 0.42), rgba(217, 119, 6, 0.14));
}

.home-landing--light .oj-strip-card__meter {
  background: rgba(231, 229, 228, 0.8);
  border-color: rgba(120, 113, 108, 0.22);
}

.home-landing--light .oj-strip-card__meter-fill {
  box-shadow: 0 0 8px rgba(15, 118, 110, 0.25);
}

.home-landing--light .oj-mini-terminal {
  background: rgba(255, 250, 244, 0.96);
  border-color: rgba(15, 118, 110, 0.2);
  box-shadow: 0 8px 22px rgba(62, 45, 28, 0.07);
}

.home-landing--light .oj-mini-terminal .oj-terminal-bar.oj-terminal-bar--mini {
  background: rgba(255, 252, 248, 0.98);
  border-bottom: 1px solid rgba(92, 68, 46, 0.12);
}

.home-landing--light .oj-mini-terminal .oj-terminal-name {
  color: #78716c;
}

.home-landing--light .oj-mini-terminal:hover {
  border-color: rgba(13, 148, 136, 0.35);
}

.home-landing--light .oj-mini-terminal--alt .oj-terminal-bar.oj-terminal-bar--mini {
  background: linear-gradient(90deg, rgba(15, 118, 110, 0.12), rgba(180, 100, 50, 0.08));
}

@keyframes ojMesh {
  to {
    transform: rotate(360deg);
  }
}

@keyframes ojGlow {
  0%,
  100% {
    transform: translate(0, 0) scale(1);
    opacity: 0.85;
  }
  50% {
    transform: translate(2%, -3%) scale(1.05);
    opacity: 1;
  }
}

@keyframes ojAurora {
  0% {
    transform: translate(-4%, -3%) rotate(0deg) scale(1);
    opacity: 0.38;
  }
  50% {
    opacity: 0.48;
  }
  100% {
    transform: translate(5%, 4%) rotate(10deg) scale(1.06);
    opacity: 0.4;
  }
}

@keyframes ojAuroraLight {
  0% {
    transform: translate(-2%, -1%) scale(1);
  }
  100% {
    transform: translate(3%, 2%) scale(1.04);
  }
}

@keyframes ojStarDriftFar {
  from {
    transform: translate(0, 0);
  }
  to {
    transform: translate(-3%, -2%);
  }
}

@keyframes ojStarDriftMid {
  from {
    transform: translate(0, 0) scale(1);
  }
  to {
    transform: translate(2.5%, 1.8%) scale(1.02);
  }
}

@keyframes ojStarDriftNear {
  from {
    transform: translate(0, 0);
  }
  to {
    transform: translate(-1.8%, 2.2%);
  }
}

@keyframes ojStarTwinkleA {
  from {
    opacity: 0.42;
  }
  to {
    opacity: 0.68;
  }
}

@keyframes ojStarTwinkleB {
  from {
    opacity: 0.5;
  }
  to {
    opacity: 0.78;
  }
}

@keyframes ojStarTwinkleC {
  from {
    opacity: 0.58;
  }
  to {
    opacity: 0.88;
  }
}

@keyframes ojMeteor {
  0%,
  82% {
    opacity: 0;
    transform: rotate(var(--meteor-rot)) translate3d(40px, 24px, 0);
  }
  84% {
    opacity: 0.85;
  }
  88% {
    opacity: 0.95;
    transform: rotate(var(--meteor-rot)) translate3d(-120px, -72px, 0);
  }
  92%,
  100% {
    opacity: 0;
    transform: rotate(var(--meteor-rot)) translate3d(-200px, -118px, 0);
  }
}

@keyframes ojBeam {
  0%,
  100% {
    opacity: 0.35;
    transform: skewX(-12deg) translateX(-4%);
  }
  50% {
    opacity: 0.65;
    transform: skewX(-12deg) translateX(6%);
  }
}

.oj-hero-inner {
  position: relative;
  z-index: 2;
  max-width: var(--app-page-width);
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(300px, 0.95fr);
  gap: 40px;
  align-items: center;
}

.oj-badge {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 20px;
  padding: 8px 14px 8px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--oj-muted);
  background: rgba(15, 23, 42, 0.65);
  border: 1px solid var(--oj-line);
}

.oj-badge-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--oj-cyan);
  box-shadow: 0 0 12px var(--oj-cyan);
  animation: ojPulse 2.4s ease-in-out infinite;
}

@keyframes ojPulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.65;
    transform: scale(0.92);
  }
}

.oj-brand {
  margin: 0;
  font-family: var(--app-font-display);
  font-size: clamp(44px, 7vw, 76px);
  font-weight: 700;
  line-height: 1.02;
  letter-spacing: -0.03em;
  /* 不用 color:transparent + background-clip:text，主题切换时部分内核会重绘失败导致「空白」 */
  color: #ecfeff;
  text-shadow:
    0 0 40px rgba(56, 189, 248, 0.35),
    0 1px 0 rgba(15, 23, 42, 0.2);
}

.oj-slogan {
  margin: 16px 0 0;
  font-size: clamp(18px, 2.4vw, 24px);
  font-weight: 600;
  line-height: 1.45;
  color: #cbd5e1;
  max-width: 520px;
}

/** 主标语：分段渐变 + 轻微流动（不支持 clip 时由 @supports 回退纯色） */
.oj-slogan--shine {
  font-size: clamp(20px, 2.85vw, 30px);
  font-weight: 700;
  line-height: 1.48;
  letter-spacing: -0.02em;
  max-width: 560px;
  filter: drop-shadow(0 2px 28px rgba(45, 212, 191, 0.12));
}

.oj-slogan__text,
.oj-slogan__accent {
  display: inline;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.oj-slogan__text {
  background-image: linear-gradient(
    118deg,
    #f8fafc 0%,
    #cbd5e1 28%,
    #e0f2fe 52%,
    #f1f5f9 100%
  );
  background-size: 200% auto;
}

.oj-slogan__accent {
  background-image: linear-gradient(
    105deg,
    #5eead4 0%,
    #2dd4bf 22%,
    #4ade80 48%,
    #86efac 72%,
    #7dd3fc 100%
  );
  background-size: 220% auto;
}

@media (prefers-reduced-motion: no-preference) {
  .oj-slogan__text {
    animation: ojSloganSheen 12s ease-in-out infinite;
  }

  .oj-slogan__accent {
    animation: ojSloganAccentFlow 9s ease-in-out infinite alternate;
  }
}

@keyframes ojSloganSheen {
  0%,
  100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

@keyframes ojSloganAccentFlow {
  0% {
    background-position: 0% 50%;
  }
  100% {
    background-position: 100% 50%;
  }
}

.oj-lead {
  margin: 14px 0 0;
  max-width: 520px;
  font-size: 15px;
  line-height: 1.85;
  color: var(--oj-muted);
}

.oj-lead--rich {
  font-size: 15px;
}

.oj-lead__gradient {
  font-weight: 600;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  background-image: linear-gradient(
    92deg,
    #94a3b8 0%,
    #5eead4 28%,
    #2dd4bf 55%,
    #86efac 88%
  );
  background-size: 180% auto;
}

@media (prefers-reduced-motion: no-preference) {
  .oj-lead__gradient {
    animation: ojLeadShimmer 8s ease-in-out infinite;
  }
}

@keyframes ojLeadShimmer {
  0%,
  100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

.oj-cta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}

.oj-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 13px 22px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 700;
  text-decoration: none;
  transition:
    transform 0.2s ease,
    box-shadow 0.25s ease,
    background 0.2s ease,
    border-color 0.2s ease;
}

.oj-btn:hover {
  transform: translateY(-2px);
}

.oj-btn--primary {
  color: #042f2e;
  background: linear-gradient(135deg, #5eead4 0%, #2dd4bf 100%);
  box-shadow: 0 12px 32px rgba(45, 212, 191, 0.25);
}

.oj-btn--ghost {
  color: var(--oj-text);
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: rgba(15, 23, 42, 0.4);
}

.oj-btn--ghost:hover {
  border-color: rgba(94, 234, 212, 0.45);
  background: rgba(15, 23, 42, 0.65);
}

.oj-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 36px;
  padding-top: 28px;
  border-top: 1px solid rgba(148, 163, 184, 0.15);
}

.oj-stat strong {
  display: block;
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 4px;
  background-image: linear-gradient(96deg, #f8fafc 0%, #bae6fd 28%, #5eead4 58%, #86efac 92%);
  background-size: 170% auto;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

@media (prefers-reduced-motion: no-preference) {
  .oj-stat strong {
    animation: ojStatLabelPulse 11s ease-in-out infinite alternate;
  }
}

@keyframes ojStatLabelPulse {
  0% {
    background-position: 0% 50%;
  }
  100% {
    background-position: 100% 50%;
  }
}

.oj-stat span {
  font-size: 12px;
  color: var(--oj-muted);
  line-height: 1.5;
}

/* 终端装饰：标签在代码区左侧留白列内浮动，不压代码；下方为迷你代码框 */
.oj-hero-right {
  position: relative;
}

.oj-code-cluster {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.oj-terminal {
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(56, 189, 248, 0.2);
  background: rgba(3, 7, 18, 0.85);
  box-shadow:
    0 24px 48px rgba(0, 0, 0, 0.45),
    0 0 0 1px rgba(255, 255, 255, 0.04) inset;
}

.oj-terminal--feature {
  position: relative;
}

.oj-terminal-mid {
  display: grid;
  grid-template-columns: minmax(100px, auto) minmax(0, 1fr);
  align-items: stretch;
  min-height: 0;
}

.oj-float-rail {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 12px;
  padding: 14px 12px;
  background:
    linear-gradient(180deg, rgba(15, 23, 42, 0.72), rgba(2, 6, 23, 0.78)),
    radial-gradient(ellipse at 16% 10%, rgba(94, 234, 212, 0.14), transparent 52%);
  border-right: 1px solid rgba(148, 163, 184, 0.2);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.oj-terminal-code-col {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.oj-terminal-code-col .oj-code {
  flex: 1 1 auto;
}

.oj-strip-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(2, 6, 23, 0.76);
  border: 1px solid rgba(148, 163, 184, 0.16);
  font-size: 11px;
  color: #cbd5e1;
  transition:
    border-color 0.24s ease,
    box-shadow 0.24s ease,
    background 0.24s ease;
}

.oj-strip-card--float {
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 7px;
  min-width: 0;
  width: 100%;
  position: relative;
  overflow: hidden;
  box-shadow:
    0 12px 24px rgba(0, 0, 0, 0.24),
    inset 0 1px 0 rgba(255, 255, 255, 0.05);
  will-change: transform;
}

.oj-strip-card--float::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(56, 189, 248, 0.1), transparent 55%, rgba(45, 212, 191, 0.08));
  pointer-events: none;
  opacity: 0.85;
}

.oj-strip-card--float > * {
  position: relative;
  z-index: 1;
}

.oj-strip-card--a {
  animation: ojFloatRailA 5.2s ease-in-out infinite;
  border-color: rgba(56, 189, 248, 0.3);
}

.oj-strip-card--b {
  animation: ojFloatRailB 5.2s ease-in-out infinite;
  animation-delay: -2.6s;
  border-color: rgba(45, 212, 191, 0.3);
}

.oj-strip-card__head {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.oj-strip-card__k {
  flex-shrink: 0;
  line-height: 1.2;
  letter-spacing: 0.03em;
  color: #94a3b8;
}

.oj-strip-card__ico {
  width: 20px;
  height: 20px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  color: #67e8f9;
  border: 1px solid rgba(103, 232, 249, 0.35);
  background: rgba(8, 47, 73, 0.55);
}

.oj-strip-card__v {
  font-size: 17px;
  font-weight: 800;
  color: var(--oj-amber);
  font-family: var(--app-font-display);
  letter-spacing: -0.02em;
}

.oj-strip-card__hint {
  font-size: 11px;
  line-height: 1.2;
  color: #9ca3af;
}

.oj-strip-card__line {
  width: 100%;
  height: 1px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(56, 189, 248, 0.55), rgba(45, 212, 191, 0.12));
}

.oj-strip-card__meter {
  width: 100%;
  height: 6px;
  border-radius: 999px;
  background: rgba(30, 41, 59, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.2);
  overflow: hidden;
}

.oj-strip-card__meter-fill {
  display: block;
  width: 57.14%;
  height: 100%;
  background: linear-gradient(90deg, #2dd4bf, #67e8f9);
  box-shadow: 0 0 10px rgba(45, 212, 191, 0.45);
}

@keyframes ojFloatRailA {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-7px);
  }
}

@keyframes ojFloatRailB {
  0%,
  100% {
    transform: translateY(-4px);
  }
  50% {
    transform: translateY(5px);
  }
}

.oj-code-minis {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.oj-mini-terminal {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(56, 189, 248, 0.16);
  background: rgba(3, 10, 24, 0.88);
  box-shadow:
    0 12px 28px rgba(0, 0, 0, 0.35),
    0 0 0 1px rgba(255, 255, 255, 0.03) inset;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.oj-mini-terminal:hover {
  border-color: rgba(56, 189, 248, 0.32);
  box-shadow:
    0 16px 36px rgba(0, 0, 0, 0.4),
    0 0 0 1px rgba(94, 234, 212, 0.08) inset;
}

.oj-mini-terminal--alt {
  border-color: rgba(94, 234, 212, 0.14);
}

.oj-code--mini {
  padding: 12px 12px 14px;
  font-size: 11px;
  line-height: 1.55;
  min-height: 4.8em;
}

.oj-terminal-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: rgba(15, 23, 42, 0.95);
  border-bottom: 1px solid rgba(51, 65, 85, 0.6);
}

.oj-terminal-bar.oj-terminal-bar--mini {
  padding: 8px 12px;
}

.oj-terminal-bar.oj-terminal-bar--mini .oj-dots {
  width: 8px;
  height: 8px;
  box-shadow:
    11px 0 0 #fbbf24,
    22px 0 0 #4ade80;
}

.oj-dots {
  width: 10px;
  height: 10px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #f87171;
  box-shadow:
    14px 0 0 #fbbf24,
    28px 0 0 #4ade80;
}

.oj-terminal-name {
  margin-left: auto;
  font-size: 12px;
  color: #64748b;
  font-family: ui-monospace, 'Cascadia Code', monospace;
}

.oj-code {
  margin: 0;
  padding: 18px 16px 20px;
  font-size: 12px;
  line-height: 1.65;
  overflow-x: auto;
  font-family: ui-monospace, 'Cascadia Code', 'Consolas', monospace;
  color: #cbd5e1;
}

.tok-k {
  color: #c084fc;
}
.tok-s {
  color: #86efac;
}
.tok-t {
  color: #7dd3fc;
}
.tok-f {
  color: #fcd34d;
}
.tok-p {
  color: #94a3b8;
}
.tok-c {
  color: #64748b;
  font-style: italic;
}
.tok-n {
  color: #f472b6;
}
.tok-m {
  color: #fdba74;
}

.oj-run {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: rgba(6, 78, 59, 0.35);
  border-top: 1px solid rgba(52, 211, 153, 0.2);
}

.oj-ac {
  font-size: 13px;
  font-weight: 700;
  color: #6ee7b7;
}

.oj-meta {
  font-size: 12px;
  color: #64748b;
}

/* 阶梯入场：默认即可见，仅用位移/模糊，避免 opacity:0 + 动画异常导致整页空白 */
.hero-in {
  opacity: 1;
  animation: heroIn 0.85s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.hero-in--1 {
  animation-delay: 0.06s;
}
.hero-in--2 {
  animation-delay: 0.14s;
}
.hero-in--3 {
  animation-delay: 0.24s;
}
.hero-in--4 {
  animation-delay: 0.34s;
}
.hero-in--5 {
  animation-delay: 0.44s;
}
.hero-in--6 {
  animation-delay: 0.54s;
}

@keyframes heroIn {
  from {
    transform: translate3d(0, 14px, 0);
  }
  to {
    transform: translate3d(0, 0, 0);
  }
}

/* ========== 下方：跟随全站主题 ========== */
.home-body {
  max-width: var(--app-page-width);
  margin: 0 auto;
  padding: 28px 20px 60px;
}

.home-section-head {
  margin-bottom: 28px;
}

.home-section-head--tight {
  margin-top: 52px;
  margin-bottom: 26px;
}

.home-section-eyebrow {
  display: inline-flex;
  align-items: center;
  margin: 0 0 16px;
  padding: 7px 16px;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.14em;
  color: var(--app-accent);
  background: linear-gradient(
    135deg,
    rgba(15, 140, 122, 0.14) 0%,
    rgba(15, 140, 122, 0.05) 100%
  );
  border: 1px solid rgba(15, 140, 122, 0.28);
  border-radius: 999px;
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.45) inset;
}

.home-landing:not(.home-landing--light) .home-section-eyebrow {
  background: linear-gradient(
    135deg,
    rgba(83, 219, 194, 0.14) 0%,
    rgba(83, 219, 194, 0.04) 100%
  );
  border-color: rgba(83, 219, 194, 0.28);
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.06) inset;
}

.home-section-title {
  margin: 0;
  font-size: clamp(30px, 5vw, 44px);
  font-family: var(--app-font-display);
  font-weight: 700;
  line-height: 1.12;
  letter-spacing: -0.03em;
  color: var(--app-text);
}

/** 主页正文区标题：柔和渐变（深色页更亮，浅色页偏青橙） */
.home-landing:not(.home-landing--light) .home-section-title {
  background-image: linear-gradient(
    96deg,
    #f8fafc 0%,
    #e2e8f0 18%,
    #99f6e4 42%,
    #5eead4 62%,
    #fef9c3 88%
  );
  background-size: 160% auto;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.home-landing--light .home-section-title {
  background-image: linear-gradient(94deg, #134e4a 0%, #0f766e 35%, #9a3412 92%);
  background-size: 140% auto;
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

@media (prefers-reduced-motion: no-preference) {
  .home-landing:not(.home-landing--light) .home-section-title {
    animation: ojSectionTitleGlow 14s ease-in-out infinite alternate;
  }

  .home-landing--light .home-section-title {
    animation: ojSectionTitleGlow 16s ease-in-out infinite alternate;
  }
}

@keyframes ojSectionTitleGlow {
  0% {
    background-position: 0% 50%;
  }
  100% {
    background-position: 100% 50%;
  }
}

.home-section-title::after {
  content: '';
  display: block;
  width: min(88px, 22vw);
  height: 4px;
  margin-top: 16px;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--app-accent), var(--app-accent-2));
  opacity: 0.9;
}

.home-section-lead {
  margin: 18px 0 0;
  font-size: 17px;
  line-height: 1.75;
  color: var(--app-text-muted);
  max-width: 560px;
  letter-spacing: 0.01em;
}

.oj-quick {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 0;
}

.oj-quick-card {
  --qc-a: rgba(13, 148, 136, 0.35);
  --qc-b: rgba(251, 191, 36, 0.2);
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 22px 18px;
  border-radius: 18px;
  text-decoration: none;
  color: inherit;
  border: 1px solid var(--app-border);
  background: var(--app-surface);
  box-shadow: var(--app-card-shadow);
  overflow: hidden;
  isolation: isolate;
  transition:
    transform 0.35s cubic-bezier(0.22, 1, 0.36, 1),
    border-color 0.25s ease,
    box-shadow 0.35s ease;
}

.oj-quick-card--t0 {
  --qc-a: rgba(13, 148, 136, 0.4);
  --qc-b: rgba(45, 212, 191, 0.18);
}
.oj-quick-card--t1 {
  --qc-a: rgba(59, 130, 246, 0.35);
  --qc-b: rgba(129, 140, 248, 0.22);
}
.oj-quick-card--t2 {
  --qc-a: rgba(217, 119, 6, 0.3);
  --qc-b: rgba(251, 191, 36, 0.25);
}
.oj-quick-card--t3 {
  --qc-a: rgba(236, 72, 153, 0.22);
  --qc-b: rgba(167, 139, 250, 0.22);
}

.oj-quick-card__aurora {
  position: absolute;
  inset: -55%;
  background: conic-gradient(from 120deg at 50% 50%, var(--qc-a), transparent 40%, var(--qc-b), transparent 78%);
  opacity: 0;
  animation: ojQuickAurora 14s linear infinite;
  pointer-events: none;
  z-index: 0;
  transition: opacity 0.4s ease;
}

.oj-quick-card:hover .oj-quick-card__aurora {
  opacity: 0.55;
}

.oj-quick-card__edge {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);
  pointer-events: none;
  z-index: 1;
}

.oj-quick-card > :not(.oj-quick-card__aurora):not(.oj-quick-card__edge) {
  position: relative;
  z-index: 2;
}

.oj-quick-card:hover {
  transform: translateY(-6px) scale(1.01);
  border-color: rgba(15, 140, 122, 0.38);
  box-shadow:
    0 22px 44px rgba(15, 140, 122, 0.14),
    0 0 0 1px rgba(15, 140, 122, 0.08);
}

.home-landing:not(.home-landing--light) .oj-quick-card:hover {
  border-color: rgba(45, 212, 191, 0.38);
  box-shadow:
    0 24px 56px rgba(0, 0, 0, 0.4),
    0 0 0 1px rgba(45, 212, 191, 0.12);
}

.oj-quick-glyph {
  font-size: 22px;
  line-height: 1;
  opacity: 0.95;
  filter: drop-shadow(0 0 12px rgba(13, 148, 136, 0.25));
  transition: transform 0.35s ease;
}

.oj-quick-card:hover .oj-quick-glyph {
  transform: scale(1.08) rotate(-6deg);
}

.home-landing:not(.home-landing--light) .oj-quick-glyph {
  filter: drop-shadow(0 0 14px rgba(45, 212, 191, 0.35));
}

.oj-quick-card strong {
  font-size: 17px;
  font-weight: 700;
  font-family: var(--app-font-display);
}

.oj-quick-card span:last-child {
  font-size: 13px;
  color: var(--app-text-muted);
  line-height: 1.5;
}

@keyframes ojQuickAurora {
  to {
    transform: rotate(360deg);
  }
}

.panel {
  position: relative;
  border: 1px solid var(--app-border);
  background: var(--app-surface);
  box-shadow: var(--app-card-shadow);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin-top: 0;
}

.oj-pillar {
  border-radius: 24px;
  padding: 24px 22px 26px;
  overflow: hidden;
  isolation: isolate;
  transition:
    transform 0.4s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.4s ease,
    border-color 0.3s ease;
}

.oj-pillar--a {
  --pillar-h1: rgba(13, 148, 136, 0.2);
  --pillar-h2: rgba(45, 212, 191, 0.12);
}
.oj-pillar--b {
  --pillar-h1: rgba(59, 130, 246, 0.18);
  --pillar-h2: rgba(129, 140, 248, 0.12);
}
.oj-pillar--c {
  --pillar-h1: rgba(217, 119, 6, 0.16);
  --pillar-h2: rgba(251, 191, 36, 0.12);
}

.oj-pillar__bg {
  position: absolute;
  inset: -40%;
  background:
    radial-gradient(ellipse 60% 50% at 80% 0%, var(--pillar-h1), transparent 55%),
    radial-gradient(ellipse 50% 45% at 10% 100%, var(--pillar-h2), transparent 50%);
  opacity: 0.85;
  animation: ojPillarBlob 16s ease-in-out infinite;
  pointer-events: none;
  z-index: 0;
}

.oj-pillar--b .oj-pillar__bg {
  animation-delay: -5s;
}
.oj-pillar--c .oj-pillar__bg {
  animation-delay: -10s;
}

.oj-pillar:hover {
  transform: translateY(-5px);
  border-color: rgba(15, 140, 122, 0.28);
  box-shadow:
    0 28px 56px rgba(15, 23, 42, 0.12),
    0 0 0 1px rgba(13, 148, 136, 0.1);
}

.home-landing:not(.home-landing--light) .oj-pillar:hover {
  border-color: rgba(45, 212, 191, 0.22);
  box-shadow: 0 28px 64px rgba(0, 0, 0, 0.45);
}

.oj-pillar__tag,
.oj-pillar h3,
.oj-pillar__body,
.oj-pillar__points {
  position: relative;
  z-index: 1;
}

.oj-pillar__tag {
  margin: 0 0 12px;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--app-accent);
}

.oj-pillar h3 {
  margin: 0;
  font-size: clamp(20px, 2.2vw, 24px);
  line-height: 1.22;
  font-family: var(--app-font-display);
  font-weight: 700;
}

.oj-pillar__body {
  margin: 12px 0 0;
  color: var(--app-text-muted);
  line-height: 1.75;
  font-size: 14px;
}

.oj-pillar__points {
  margin: 16px 0 0;
  padding: 0 0 0 1.1em;
  font-size: 13px;
  color: var(--app-text-muted);
  line-height: 1.7;
}

.oj-pillar__points li {
  margin-bottom: 6px;
}

.oj-pillar__points li::marker {
  color: var(--app-accent-2);
}

@keyframes ojPillarBlob {
  0%,
  100% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(4%, -3%) scale(1.05);
  }
}

.oj-advantages {
  position: relative;
  margin-top: 36px;
  border-radius: 28px;
  padding: 0;
  overflow: hidden;
}

.oj-advantages__bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    125deg,
    rgba(13, 148, 136, 0.09) 0%,
    transparent 42%,
    rgba(59, 130, 246, 0.07) 62%,
    rgba(251, 191, 36, 0.06) 100%
  );
  opacity: 1;
  animation: ojAdvGradient 18s ease-in-out infinite alternate;
  pointer-events: none;
}

.home-landing:not(.home-landing--light) .oj-advantages__bg {
  background: linear-gradient(
    125deg,
    rgba(45, 212, 191, 0.08) 0%,
    transparent 38%,
    rgba(56, 189, 248, 0.07) 55%,
    rgba(129, 140, 248, 0.06) 100%
  );
}

.oj-advantages__inner {
  position: relative;
  z-index: 1;
  padding: 28px 26px 30px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.oj-advantages__intro {
  max-width: 640px;
}

.oj-advantages__title {
  margin: 12px 0 0;
  font-size: clamp(30px, 4.8vw, 42px);
  line-height: 1.14;
  font-family: var(--app-font-display);
  font-weight: 700;
  letter-spacing: -0.03em;
  color: var(--app-text);
}

.oj-advantages__title::after {
  content: '';
  display: block;
  width: min(88px, 22vw);
  height: 4px;
  margin-top: 16px;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--app-accent), var(--app-accent-2));
  opacity: 0.9;
}

.oj-advantages__lead {
  margin: 18px 0 0;
  font-size: 17px;
  line-height: 1.75;
  color: var(--app-text-muted);
  letter-spacing: 0.01em;
}

.oj-advantages__grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.oj-adv-card {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  padding: 18px 16px;
  border-radius: 18px;
  background: var(--app-surface-soft);
  border: 1px solid var(--app-border);
  transition:
    transform 0.3s ease,
    border-color 0.25s ease,
    box-shadow 0.3s ease,
    background 0.25s ease;
}

.oj-adv-card:hover {
  transform: translateY(-3px);
  border-color: rgba(13, 148, 136, 0.3);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.08);
  background: var(--app-surface);
}

.home-landing:not(.home-landing--light) .oj-adv-card {
  background: rgba(15, 23, 42, 0.5);
}

.home-landing:not(.home-landing--light) .oj-adv-card:hover {
  border-color: rgba(45, 212, 191, 0.28);
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.35);
}

.oj-adv-card__icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  border-radius: 12px;
  background: linear-gradient(145deg, rgba(13, 148, 136, 0.18), rgba(45, 212, 191, 0.08));
  border: 1px solid rgba(13, 148, 136, 0.2);
}

.home-landing:not(.home-landing--light) .oj-adv-card__icon {
  background: linear-gradient(145deg, rgba(45, 212, 191, 0.15), rgba(56, 189, 248, 0.08));
  border-color: rgba(45, 212, 191, 0.2);
}

.oj-adv-card__text {
  min-width: 0;
}

.oj-adv-card__text strong {
  display: block;
  font-size: 15px;
  font-family: var(--app-font-display);
  font-weight: 700;
  line-height: 1.35;
}

.oj-adv-card__text span {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--app-text-muted);
}

@keyframes ojAdvGradient {
  0% {
    transform: scale(1) translate(0, 0);
    opacity: 0.85;
  }
  100% {
    transform: scale(1.06) translate(-2%, 1%);
    opacity: 1;
  }
}

/* 滚动进入视口：由 useScrollReveal 添加 .scroll-reveal--in */
[data-scroll-reveal] {
  opacity: 0;
  transform: translate3d(0, 40px, 0);
  transition:
    opacity 0.9s cubic-bezier(0.16, 1, 0.3, 1),
    transform 0.9s cubic-bezier(0.16, 1, 0.3, 1);
  transition-delay: var(--sr-delay, 0s);
  will-change: opacity, transform;
}

[data-scroll-reveal].scroll-reveal--in {
  opacity: 1;
  transform: translate3d(0, 0, 0);
  will-change: auto;
}

.oj-highlight-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin: 28px 0 8px;
}

.oj-highlight-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 20px 18px;
  border-radius: 18px;
}

.oj-highlight-card__num {
  font-size: clamp(26px, 4vw, 34px);
  font-weight: 800;
  font-family: var(--app-font-display);
  line-height: 1;
  letter-spacing: -0.03em;
  color: var(--app-accent);
}

.oj-highlight-card__label {
  font-size: 14px;
  font-weight: 700;
}

.oj-highlight-card__sub {
  font-size: 12px;
  color: var(--app-text-muted);
}

.oj-path {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 4px;
}

.oj-path-step {
  position: relative;
  padding: 22px 20px 24px;
  border-radius: 22px;
  overflow: hidden;
}

.oj-path-step__n {
  display: inline-block;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.2em;
  color: var(--app-accent-2);
  margin-bottom: 10px;
}

.oj-path-step h3 {
  margin: 0;
  font-size: 18px;
  font-family: var(--app-font-display);
  font-weight: 700;
  line-height: 1.25;
}

.oj-path-step p {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--app-text-muted);
}

.oj-path-step__hint {
  display: block;
  margin-top: 14px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: var(--app-text-subtle);
  text-transform: uppercase;
}

.oj-pipeline {
  padding: 22px 20px;
  border-radius: 22px;
  margin-top: 4px;
}

.oj-pipeline__row {
  display: flex;
  flex-wrap: wrap;
  align-items: stretch;
  justify-content: center;
  gap: 8px 6px;
}

.oj-pipeline__step {
  flex: 1 1 200px;
  max-width: 260px;
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 12px 10px;
  border-radius: 14px;
  background: var(--app-surface-soft);
  border: 1px solid var(--app-border);
}

.home-landing:not(.home-landing--light) .oj-pipeline__step {
  background: rgba(15, 23, 42, 0.45);
}

.oj-pipeline__dot {
  flex-shrink: 0;
  width: 10px;
  height: 10px;
  margin-top: 5px;
  border-radius: 50%;
  background: linear-gradient(145deg, var(--app-accent), var(--app-accent-2));
  box-shadow: 0 0 14px rgba(13, 148, 136, 0.35);
}

.home-landing:not(.home-landing--light) .oj-pipeline__dot {
  box-shadow: 0 0 14px rgba(45, 212, 191, 0.35);
}

.oj-pipeline__body {
  min-width: 0;
}

.oj-pipeline__body strong {
  display: block;
  font-size: 14px;
  font-family: var(--app-font-display);
  font-weight: 700;
}

.oj-pipeline__body span {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.55;
  color: var(--app-text-muted);
}

.oj-pipeline__arrow {
  flex: 0 0 auto;
  align-self: center;
  padding: 0 4px;
  font-size: 18px;
  color: var(--app-accent);
  opacity: 0.55;
}

.oj-scenarios {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 4px;
}

.oj-scenario {
  padding: 22px 20px;
  border-radius: 20px;
}

.oj-scenario__icon {
  display: block;
  font-size: 28px;
  line-height: 1;
  margin-bottom: 12px;
}

.oj-scenario h3 {
  margin: 0;
  font-size: 17px;
  font-family: var(--app-font-display);
  font-weight: 700;
}

.oj-scenario p {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.65;
  color: var(--app-text-muted);
}

@supports not ((-webkit-background-clip: text) or (background-clip: text)) {
  .oj-slogan__text {
    color: #f1f5f9 !important;
    background: none !important;
  }

  .oj-slogan__accent {
    color: #5eead4 !important;
    background: none !important;
    filter: none !important;
  }

  .oj-lead__gradient {
    color: #5eead4 !important;
    background: none !important;
  }

  .home-landing:not(.home-landing--light) .home-section-title,
  .home-landing--light .home-section-title {
    color: var(--app-text) !important;
    background: none !important;
  }

  .oj-stat strong {
    color: #f1f5f9 !important;
    background: none !important;
  }

  .home-landing--light .oj-stat strong {
    color: #0f172a !important;
  }
}

@media (prefers-reduced-motion: reduce) {
  .oj-slogan__text,
  .oj-slogan__accent,
  .oj-lead__gradient,
  .home-landing .home-section-title {
    animation: none !important;
    background-position: 50% 50% !important;
  }

  .oj-stat strong {
    animation: none !important;
  }

  .oj-mesh,
  .oj-glow,
  .oj-stars,
  .oj-stars *,
  .oj-beam,
  .oj-badge-dot,
  .oj-brand {
    animation: none !important;
  }

  .oj-stars-aurora {
    opacity: 0.28;
  }

  .oj-stars-layer--far {
    opacity: 0.52;
  }

  .oj-stars-layer--mid {
    opacity: 0.58;
  }

  .oj-stars-layer--near {
    opacity: 0.68;
  }

  .oj-stars-meteor {
    opacity: 0 !important;
  }

  .oj-mini-terminal:hover {
    transform: none;
  }

  .oj-strip-card--float {
    animation: none !important;
  }

  .oj-quick-card__aurora,
  .oj-pillar__bg,
  .oj-advantages__bg {
    animation: none !important;
  }

  .oj-quick-card:hover,
  .oj-pillar:hover,
  .oj-adv-card:hover {
    transform: none !important;
  }

  [data-scroll-reveal] {
    opacity: 1 !important;
    transform: none !important;
    transition: none !important;
  }

  .hero-in {
    animation: none !important;
    opacity: 1 !important;
    transform: none !important;
    filter: none !important;
  }
}

@media (max-width: 1024px) {
  .oj-hero-inner {
    grid-template-columns: 1fr;
    gap: 36px;
  }

  .oj-hero-right {
    max-width: 480px;
    margin: 0 auto;
    width: 100%;
  }

  .oj-code-minis {
    grid-template-columns: 1fr;
  }

  .oj-quick {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .oj-highlight-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .feature-grid {
    grid-template-columns: 1fr;
  }

  .oj-path {
    grid-template-columns: 1fr;
  }

  .oj-scenarios {
    grid-template-columns: 1fr;
  }

  .oj-pipeline__arrow {
    display: none;
  }

  .oj-advantages__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .oj-stats {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .oj-hero {
    padding: 32px 16px 40px;
    min-height: auto;
  }

  .oj-terminal-mid {
    grid-template-columns: 1fr;
  }

  .oj-float-rail {
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: center;
    align-items: stretch;
    border-right: none;
    border-bottom: 1px solid rgba(51, 65, 85, 0.55);
    padding: 10px;
    gap: 8px;
  }

  .home-landing--light .oj-float-rail {
    border-bottom-color: rgba(92, 68, 46, 0.12);
  }

  .oj-strip-card--float {
    flex: 1 1 140px;
    max-width: 100%;
    gap: 6px;
  }

  .oj-strip-card__head {
    align-items: center;
  }

  .oj-strip-card__meter {
    height: 5px;
  }

  .oj-cta {
    flex-direction: column;
  }

  .oj-btn {
    width: 100%;
  }

  .oj-quick {
    grid-template-columns: 1fr;
  }

  .oj-advantages__grid {
    grid-template-columns: 1fr;
  }

  .oj-highlight-strip {
    grid-template-columns: 1fr;
  }

  .home-body {
    padding: 20px 14px 48px;
  }
}
</style>
