<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { adminApi } from '../api/admin.js';
import { shortAddress, formatDate } from '../utils/format.js';

const { t } = useI18n();
const router = useRouter();
const loading = ref(false);
const rows = ref([]);
const page = ref(1);
const pageSize = ref(10);

// 前端分页：后端返回全量，这里按页切片
const paged = computed(() => {
  const start = (page.value - 1) * pageSize.value;
  return rows.value.slice(start, start + pageSize.value);
});
const total = computed(() => rows.value.length);

async function load() {
  loading.value = true;
  try {
    rows.value = (await adminApi.getUsers()) || [];
  } catch (e) {
    ElMessage.error(e.message);
  } finally {
    loading.value = false;
  }
}

function viewProofs(row) {
  router.push({ path: '/proofs', query: { address: row.address } });
}

onMounted(load);
</script>

<template>
  <el-card>
    <el-table :data="paged" v-loading="loading" stripe>
      <el-table-column prop="id" :label="t('users.id')" width="80" />
      <el-table-column :label="t('users.address')" min-width="200">
        <template #default="{ row }">
          <span class="mono" :title="row.address">{{ shortAddress(row.address) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="creditBalance" :label="t('users.balance')" width="120" />
      <el-table-column prop="purchaseTotal" :label="t('users.purchaseTotal')" width="120" />
      <el-table-column :label="t('users.createdAt')" width="180">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column :label="t('users.action')" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewProofs(row)">{{ t('users.viewProofs') }}</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty :description="t('users.empty')" />
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
.mono { font-family: "SFMono-Regular", Consolas, monospace; }
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
