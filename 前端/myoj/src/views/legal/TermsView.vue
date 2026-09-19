<template>
  <div class="legal-page">
    <header class="legal-hero panel">
      <p class="legal-hero__eyebrow">Myoj Charter</p>
      <h1>服务协议</h1>
      <p>请在使用平台前仔细阅读本协议，继续使用即视为您已理解并同意以下条款。</p>
      <p class="legal-meta">更新日期：2026-04-02</p>
    </header>

    <section class="legal-card panel">
      <h2>1. 协议的适用范围</h2>
      <p>本协议适用于您访问、注册、登录与使用码跃 OJ（以下简称“平台”）的全部服务，包括题库、比赛、社区、评测与账户管理功能。</p>
    </section>

    <section class="legal-card panel">
      <h2>2. 账户与安全</h2>
      <ul>
        <li>您应提供真实、合法、有效的注册信息并妥善保管账户凭证。</li>
        <li>禁止转借、出租、出售账户，或以任何方式干扰平台正常运行。</li>
        <li>若发现账户异常，请及时通过平台渠道反馈，我们将协助处理。</li>
      </ul>
    </section>

    <section class="legal-card panel">
      <h2>3. 用户行为规范</h2>
      <ul>
        <li>不得发布违法、侵权、侮辱性或破坏社区秩序的内容。</li>
        <li>不得恶意刷提交、攻击评测系统、干扰比赛公平性。</li>
        <li>不得使用自动化工具批量抓取或滥用平台接口。</li>
      </ul>
    </section>

    <section class="legal-card panel">
      <h2>4. 内容与知识产权</h2>
      <p>平台中的题目、题解、界面设计、商标及相关内容受法律保护。未经授权，不得复制、传播或用于商业用途。</p>
    </section>

    <section class="legal-card panel">
      <h2>5. 服务变更与中断</h2>
      <p>基于产品升级、系统维护或安全需要，平台可对功能进行调整，并尽量提前通知。对于不可抗力导致的服务中断，平台将尽合理努力恢复。</p>
    </section>

    <section class="legal-card panel">
      <h2>6. 违约与责任</h2>
      <p>若您违反本协议，平台有权视情况采取警告、限制功能、暂停或终止账户等措施；涉及违法行为的，平台将依法配合相关机关处理。</p>
    </section>

    <section class="legal-card panel">
      <h2>7. 协议更新</h2>
      <p>本协议可能根据法律法规或业务变化进行更新。更新后将通过页面公示，继续使用平台即视为接受更新后的协议内容。</p>
    </section>
  </div>
</template>

<style scoped>
.legal-page {
  max-width: 980px;
  margin: 0 auto;
  padding: 28px 20px 44px;
  color: var(--app-text);
}

.panel {
  border: 1px solid var(--app-border);
  background: var(--app-surface);
  box-shadow: var(--app-card-shadow);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
}

.legal-hero,
.legal-card {
  border-radius: 26px;
}

.legal-hero {
  margin-bottom: 16px;
  padding: 28px;
  overflow: hidden;
  position: relative;
}

.legal-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at top left, rgba(255, 138, 61, 0.18), transparent 30%),
    radial-gradient(circle at bottom right, rgba(15, 140, 122, 0.16), transparent 28%);
  pointer-events: none;
}

.legal-hero__eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--app-accent);
  font-weight: 700;
}

.legal-hero h1,
.legal-card h2 {
  position: relative;
  margin: 0;
  font-family: var(--app-font-display);
}

.legal-hero h1 {
  font-size: clamp(34px, 4vw, 52px);
  line-height: 1.08;
}

.legal-hero > p {
  position: relative;
  margin: 12px 0 0;
  line-height: 1.85;
  color: var(--app-text-muted);
}

.legal-meta {
  font-size: 12px;
  color: var(--app-text-subtle) !important;
}

.legal-card {
  margin-top: 12px;
  padding: 20px 22px;
}

.legal-card h2 {
  font-size: 24px;
  line-height: 1.2;
}

.legal-card p,
.legal-card li {
  margin-top: 10px;
  line-height: 1.9;
  color: var(--app-text-muted);
}

.legal-card ul {
  margin: 0;
  padding-left: 20px;
}

@media (max-width: 640px) {
  .legal-page {
    padding: 20px 14px 40px;
  }

  .legal-hero,
  .legal-card {
    border-radius: 22px;
    padding: 20px;
  }
}
</style>
