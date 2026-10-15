<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage, ElMessageBox } from 'element-plus';
import { adminApi } from '../api/admin.js';
import { isAmount } from '../utils/validate.js';
import { formatWei, shortHash } from '../utils/format.js';

const { t } = useI18n();
const loading = ref(false);
const rateWei = ref(null); // 当前汇率（wei 字符串）
const form = reactive({ rate: '' });
const submitting = ref(false);

// 查询当前链上汇率（读 DocToken.ethPerToken）
async function load() {
  loading.value = true;
  try {
    const data = await adminApi.getExchangeRate();
    rateWei.value = data && data.ethPerToken != null ? data.ethPerToken : null;
  } catch (e) {
    ElMessage.error(e.message);
  } finally {
    loading.value = false;
  }
}

// 调整汇率：前端校验 → 二次确认（链上交易）→ 提交后刷新当前汇率
async function doUpdate() {
  const rate = form.rate.trim();
  if (!isAmount(rate)) {
    ElMessage.warning(t('rate.invalid'));
    return;
  }
  try {
    await ElMessageBox.confirm(t('rate.confirm', { rate }), t('common.tip'), {
      type: 'warning',
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
    });
  } catch {
    return; // 用户取消
  }
  submitting.value = true;
  try {
    const data = await adminApi.setExchangeRate(rate);
    ElMessage.success(t('rate.success', { hash: shortHash(data.txHash) }));
    form.rate = '';
    load();
  } catch (e) {
    ElMessage.error(e.message);
  } finally {
    submitting.value = false;
  }
}

onMounted(load);
</script>

<template>
  <el-row :gutter="16">
    <el-col :xs="24" :md="10">
      <el-card v-loading="loading">
        <template #header><span>{{ t('rate.current') }}</span></template>
        <div v-if="rateWei" class="rate-box">
          <div class="rate-value">{{ formatWei(rateWei) }}</div>
          <div class="rate-unit">{{ t('rate.currentUnit') }}</div>
        </div>
        <el-empty v-else-if="!loading" :description="t('rate.loadFailed')" />
        <el-button class="refresh" :icon="'Refresh'" circle @click="load" />
      </el-card>
    </el-col>

    <el-col :xs="24" :md="14">
      <el-card>
        <template #header><span>{{ t('rate.set') }}</span></template>
        <el-form label-position="top" @keyup.enter="doUpdate">
          <el-form-item :label="t('rate.newRate')">
            <el-input v-model="form.rate" :placeholder="t('rate.ratePh')" clearable />
          </el-form-item>
          <div class="tip">{{ t('rate.tip') }}</div>
          <el-button type="primary" :loading="submitting" @click="doUpdate">
            {{ t('rate.submit') }}
          </el-button>
        </el-form>
      </el-card>
    </el-col>
  </el-row>
</template>

<style scoped>
.rate-box { padding: 24px 0 40px; text-align: center; }
.rate-value { font-size: 36px; font-weight: 600; word-break: break-all; }
.rate-unit { color: #909399; margin-top: 8px; }
.refresh { margin-top: 8px; }
.tip { color: #909399; font-size: 12px; line-height: 1.6; margin-bottom: 16px; }
</style>
