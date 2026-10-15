<script setup>
import { useI18n } from 'vue-i18n';
import { LANGS, lang, setLang } from '../i18n/index.js';

const { t } = useI18n();
</script>

<template>
  <el-dropdown trigger="click" @command="setLang">
    <span class="lang-btn">
      <el-icon><Globe /></el-icon>
      <span>{{ lang === 'zh-CN' ? t('lang.zh') : t('lang.en') }}</span>
      <el-icon><ArrowDown /></el-icon>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="item in LANGS"
          :key="item.value"
          :command="item.value"
          :disabled="item.value === lang"
        >
          {{ item.label }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped>
.lang-btn {
  display: inline-flex; align-items: center; gap: 4px;
  cursor: pointer; color: #475569; font-size: 14px; outline: none;
  padding: 4px 8px; border-radius: 4px;
}
.lang-btn:hover { background: #f1f5f9; }
</style>
