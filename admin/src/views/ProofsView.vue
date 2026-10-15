<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { adminApi } from '../api/admin.js';
import { shortAddress, shortHash, formatBytes, formatDate } from '../utils/format.js';

const { t } = useI18n();
const route = useRoute();
const loading = ref(false);
const rows = ref([]);
// 地址筛选：从路由 query 预填（用户页「查看存证」带过来），可再修改
const addressFilter = ref((route.query.address || '').trim());
const page = ref(1);
const pageSize = ref(10);

const paged = computed(() => {
  const start = (page.value - 1) * pageSize.value;
  return rows.value.slice(start, start + pageSize.value);
});
const total = computed(() => rows.value.length);

async function load() {
  loading.value = true;
  try {
    rows.value = (await adminApi.getProofs(addressFilter.value || undefined)) || [];
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

function onMountedInit() {
  // 若带了 address 查询参数，加载对应筛选结果
  load();
}
onMounted(onMountedInit);

const statusTag = { PENDING: 'warning', STORED: 'success' };
const statusLabel = computed(() => ({
  PENDING: t('proofs.statusPending'),
  STORED: t('proofs.statusStored'),
}));
</script>

<template>
  <el-card>
    <div class="toolbar">
      <el-input
        v-model="addressFilter"
        :placeholder="t('proofs.filterPh')"
        clearable
        class="addr-input"
        @keyup.enter="search"
        @clear="search"
      />
      <el-button type="primary" @click="search">{{ t('proofs.search') }}</el-button>
    </div>

    <el-table :data="paged" v-loading="loading" stripe>
      <el-table-column prop="id" :label="t('proofs.id')" width="70" />
      <el-table-column :label="t('proofs.file')" min-width="160">
        <template #default="{ row }">
          <div>{{ row.fileName }}</div>
          <div class="sub">{{ formatBytes(row.fileSize) }}</div>
        </template>
      </el-table-column>
      <el-table-column :label="t('proofs.hash')" min-width="160">
        <template #default="{ row }">
          <span class="mono" :title="row.fileHash">{{ shortHash(row.fileHash) }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('proofs.owner')" min-width="140">
        <template #default="{ row }">
          <span class="mono" :title="row.ownerAddress">{{ shortAddress(row.ownerAddress) }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('proofs.status')" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag[row.status] || 'info'">{{ statusLabel[row.status] || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('proofs.txHash')" min-width="140">
        <template #default="{ row }">
          <span v-if="row.txHash" class="mono" :title="row.txHash">{{ shortHash(row.txHash) }}</span>
          <span v-else class="sub">-</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('proofs.storedAt')" width="170">
        <template #default="{ row }">{{ formatDate(row.storedAt) }}</template>
      </el-table-column>
      <template #empty>
        <el-empty :description="t('proofs.empty')" />
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
.addr-input { max-width: 360px; }
.mono { font-family: "SFMono-Regular", Consolas, monospace; }
.sub { color: #909399; font-size: 12px; }
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
