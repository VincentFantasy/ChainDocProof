<script setup>
import { ref, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { adminApi } from '../api/admin.js';
import { formatWei } from '../utils/format.js';

const { t } = useI18n();
const loading = ref(false);
const stats = ref(null);

async function load() {
  loading.value = true;
  try {
    stats.value = await adminApi.getStats();
  } catch (e) {
    ElMessage.error(e.message);
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <el-card v-loading="loading">
    <el-empty v-if="!loading && !stats" :description="t('dashboard.noData')" />
    <el-row v-else-if="stats" :gutter="16">
      <el-col :xs="24" :sm="12" :md="8" :lg="4">
        <el-statistic :title="t('dashboard.totalUsers')" :value="stats.totalUsers" />
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="4">
        <el-statistic :title="t('dashboard.totalProofs')" :value="stats.totalProofs" />
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="4">
        <el-statistic :title="t('dashboard.storedProofs')" :value="stats.storedProofs" />
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="4">
        <el-statistic :title="t('dashboard.totalPurchased')" :value="stats.totalPurchasedCredits" />
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="4">
        <el-statistic :title="t('dashboard.totalMintCount')" :value="stats.totalMintCount" />
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="4">
        <div class="stat">
          <div class="stat__title">{{ t('dashboard.mintedTotal') }}</div>
          <div class="stat__value">{{ formatWei(stats.mintedTotalWei) }}</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="8" :lg="4">
        <el-statistic :title="t('dashboard.buyCount')" :value="Number(stats.buyCount) || 0" />
      </el-col>
    </el-row>
  </el-card>
</template>

<style scoped>
.stat__title { color: #909399; font-size: 13px; }
.stat__value { font-size: 28px; font-weight: 600; margin-top: 8px; word-break: break-all; }
</style>
