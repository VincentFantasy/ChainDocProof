<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';
import { adminApi } from '../api/admin.js';
import { isAddress, isPositiveAmount } from '../utils/validate.js';
import { formatWei, shortHash, formatDate } from '../utils/format.js';

const { t } = useI18n();
const loadingMints = ref(false);
const mints = ref([]);
const buyCount = ref('0');

const form = reactive({ toAddress: '', amount: '' });
const submitting = ref(false);

async function loadMints() {
  loadingMints.value = true;
  try {
    mints.value = (await adminApi.getMints()) || [];
  } catch (e) {
    ElMessage.error(e.message);
  } finally {
    loadingMints.value = false;
  }
}

async function loadExchanges() {
  try {
    const data = await adminApi.getExchanges();
    buyCount.value = data && data.buyCount != null ? data.buyCount : '0';
  } catch {
    // 链上读取失败不阻塞页面，buyCount 保持占位
  }
}

// 发放：地址 / 数量前端校验后提交，amount 为 DocToken 十进制字符串（后端转 wei）
async function doMint() {
  const addr = form.toAddress.trim();
  const amount = form.amount.trim();
  if (!isAddress(addr)) {
    ElMessage.warning(t('mint.addrInvalid'));
    return;
  }
  if (!isPositiveAmount(amount)) {
    ElMessage.warning(t('mint.amountInvalid'));
    return;
  }
  submitting.value = true;
  try {
    const data = await adminApi.mint(addr, amount);
    ElMessage.success(t('mint.success', { hash: shortHash(data.txHash) }));
    form.amount = '';
    form.toAddress = '';
    loadMints();
    loadExchanges();
  } catch (e) {
    ElMessage.error(e.message);
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  loadMints();
  loadExchanges();
});
</script>

<template>
  <div>
    <el-row :gutter="16">
      <el-col :xs="24" :md="10">
        <el-card>
          <template #header><span>{{ t('mint.title') }}</span></template>
          <el-form label-position="top" @keyup.enter="doMint">
            <el-form-item :label="t('mint.toAddress')">
              <el-input v-model="form.toAddress" placeholder="0x..." clearable />
            </el-form-item>
            <el-form-item :label="t('mint.amount')">
              <el-input v-model="form.amount" :placeholder="t('mint.amountPh')" clearable />
              <div class="tip">{{ t('mint.amountTip') }}</div>
            </el-form-item>
            <el-button type="primary" :loading="submitting" @click="doMint">{{ t('mint.submit') }}</el-button>
          </el-form>

          <el-divider />
          <div class="exchange">
            {{ t('mint.buyCount') }}：<el-tag type="success">{{ buyCount }}</el-tag>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="14">
        <el-card>
          <template #header><span>{{ t('mint.history') }}</span></template>
          <el-table :data="mints" v-loading="loadingMints" stripe max-height="480">
            <el-table-column prop="id" :label="t('mint.tableId')" width="70" />
            <el-table-column :label="t('mint.tableTo')" min-width="150">
              <template #default="{ row }">
                <span class="mono" :title="row.toAddress">{{ row.toAddress }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="t('mint.tableAmount')" width="160">
              <template #default="{ row }">{{ formatWei(row.amount) }}</template>
            </el-table-column>
            <el-table-column :label="t('mint.tableTx')" min-width="150">
              <template #default="{ row }">
                <span v-if="row.txHash" class="mono" :title="row.txHash">{{ shortHash(row.txHash) }}</span>
                <span v-else class="sub">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="adminUsername" :label="t('mint.tableOperator')" width="110" />
            <el-table-column :label="t('mint.tableTime')" width="170">
              <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
            </el-table-column>
            <template #empty>
              <el-empty :description="t('mint.empty')" />
            </template>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.tip { color: #909399; font-size: 12px; line-height: 1.6; }
.mono { font-family: "SFMono-Regular", Consolas, monospace; word-break: break-all; }
.sub { color: #909399; }
.exchange { font-size: 14px; }
</style>
