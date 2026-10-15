<script setup>
import { ref, computed, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { adminApi } from '../api/admin.js';
import { shortAddress, shortHash, formatDate } from '../utils/format.js';

const { t } = useI18n();
const loading = ref(false);
const rows = ref([]);
const addressFilter = ref('');
const typeFilter = ref('');
const page = ref(1);
const pageSize = ref(10);

const typeOptions = computed(() => [
  { value: 'PURCHASE', label: t('credit.typePurchase') },
  { value: 'STORE', label: t('credit.typeStore') },
  { value: 'VERIFY', label: t('credit.typeVerify') },
]);
const typeLabel = computed(() => ({
  PURCHASE: t('credit.typePurchase'),
  STORE: t('credit.typeStore'),
  VERIFY: t('credit.typeVerify'),
}));

const paged = computed(() => {
  const start = (page.value - 1) * pageSize.value;
  return rows.value.slice(start, start + pageSize.value);
});
const total = computed(() => rows.value.length);

async function load() {
  loading.value = true;
  try {
    rows.value = (await adminApi.getCreditLogs(
      addressFilter.value ? addressFilter.value.trim() : undefined,
      typeFilter.value || undefined
    )) || [];
  } catch (e) {
    ElMessage.error(e.message);
  } finally {
    loading.value = false;
  }
}

function search() {
  page.value = 1;
  load();
}

onMounted(load);
</script>

<template>
  <el-card>
    <div class="toolbar">
      <el-input
        v-model="addressFilter"
        :placeholder="t('credit.filterPh')"
        clearable
        class="addr-input"
        @keyup.enter="search"
        @clear="search"
      />
      <el-select v-model="typeFilter" :placeholder="t('credit.typeFilterPh')" clearable class="type-select" @change="search">
        <el-option v-for="t2 in typeOptions" :key="t2.value" :label="t2.label" :value="t2.value" />
      </el-select>
      <el-button type="primary" @click="search">{{ t('credit.search') }}</el-button>
    </div>

    <el-table :data="paged" v-loading="loading" stripe>
      <el-table-column prop="id" :label="t('credit.id')" width="70" />
      <el-table-column :label="t('credit.address')" min-width="160">
        <template #default="{ row }">
          <span class="mono" :title="row.address">{{ shortAddress(row.address) }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('credit.type')" width="120">
        <template #default="{ row }">
          <el-tag :type="row.type === 'PURCHASE' ? 'success' : 'info'" size="small">
            {{ typeLabel[row.type] || row.type }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('credit.amount')" width="120">
        <template #default="{ row }">
          <span :class="row.amount >= 0 ? 'plus' : 'minus'">
            {{ row.amount >= 0 ? '+' : '' }}{{ row.amount }}
          </span>
        </template>
      </el-table-column>
      <el-table-column :label="t('credit.proofId')" width="110">
        <template #default="{ row }">{{ row.proofId != null ? '#' + row.proofId : '-' }}</template>
      </el-table-column>
      <el-table-column :label="t('credit.tx')" min-width="150">
        <template #default="{ row }">
          <span v-if="row.txHash" class="mono" :title="row.txHash">{{ shortHash(row.txHash) }}</span>
          <span v-else class="sub">-</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('credit.time')" width="180">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <template #empty>
        <el-empty :description="t('credit.empty')" />
      </template>
    </el-table>

    <el-pagination
      class="pager"
      layout="total, prev, pager, next, sizes"
      :total="total"
      v-model:current-page="page"
      v-model:page-size="pageSize"
      :page-sizes="[10, 20, 50, 100]"
      background
    />
  </el-card>
</template>

<style scoped>
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; }
.addr-input { max-width: 320px; }
.type-select { width: 160px; }
.mono { font-family: "SFMono-Regular", Consolas, monospace; }
.sub { color: #909399; }
.plus { color: #16a34a; }
.minus { color: #dc2626; }
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
