<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { ElMessage, ElMessageBox } from 'element-plus';
import { clientApi } from '../api/client.js';
import { useCredit } from '../composables/useCredit.js';
import { EXPLORER_TX_URL, MAX_FILE_SIZE, UPLOAD_COOLDOWN_SECONDS } from '../config.js';
import { formatBytes, formatDate, shortHash } from '../utils/format.js';

const { t } = useI18n();
const router = useRouter();
const { credit } = useCredit();

const rows = ref([]);
const listLoading = ref(false);
const uploading = ref(false);

// ===== 上传（60 秒冷却）=====
const cooldown = ref(0); // 剩余冷却秒数，>0 时禁用上传
let cooldownTimer = null;

/** 从后端 429 提示里提取等待秒数，例如 "上传过于频繁，请 60 秒后再试" → 60 */
function parseCooldownSeconds(msg) {
  const m = /(\d+)\s*秒/.exec(msg || '');
  return m ? parseInt(m[1], 10) : null;
}

function startCooldown(seconds) {
  cooldown.value = seconds;
  stopCooldown();
  cooldownTimer = setInterval(() => {
    cooldown.value = cooldown.value > 0 ? cooldown.value - 1 : 0;
    if (cooldown.value === 0) stopCooldown();
  }, 1000);
}

function stopCooldown() {
  if (cooldownTimer) {
    clearInterval(cooldownTimer);
    cooldownTimer = null;
  }
}
onBeforeUnmount(stopCooldown);

/** 上传文件：前端校验大小 → 提交后端算 SHA-256 建 PENDING 记录 */
async function onFileChange(uploadFile) {
  const file = uploadFile && uploadFile.raw;
  if (!file || uploading.value) return;
  if (file.size > MAX_FILE_SIZE) {
    ElMessage.error(t('proofs.limitExceeded'));
    return;
  }
  uploading.value = true;
  try {
    await clientApi.uploadProof(file);
    ElMessage.success(t('proofs.uploaded'));
    startCooldown(UPLOAD_COOLDOWN_SECONDS);
    await reload();
  } catch (e) {
    if (e.code === 429) {
      // 后端冷却限流：按提示秒数倒计时
      const secs = parseCooldownSeconds(e.message);
      if (secs) startCooldown(secs);
      ElMessage.warning(secs ? t('proofs.cooldown', { seconds: secs }) : e.message);
    } else {
      ElMessage.error(e.message);
    }
  } finally {
    uploading.value = false;
  }
}

// ===== 存证 / 校验 =====
/** 存证上链：PENDING → 扣 1 次 → STORED（含 txHash） */
async function doStore(row) {
  if ((credit.value && credit.value.balance || 0) < 1) {
    ElMessage.warning(t('proofs.noCredit'));
    return;
  }
  try {
    await ElMessageBox.confirm(t('proofs.storeConfirm'), t('common.tip'), {
      type: 'warning',
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
    });
  } catch {
    return;
  }
  row.storeLoading = true;
  try {
    await clientApi.storeProof(row.id);
    ElMessage.success(t('proofs.storeSuccess'));
    await reload();
    useCredit().refresh();
  } catch (e) {
    ElMessage.error(e.message);
  } finally {
    row.storeLoading = false;
  }
}

// 校验走隐藏 file input：点「校验」记录目标行，选完文件再确认提交
const verifyInput = ref(null);
let verifyTarget = null;

function clickVerify(row) {
  if ((credit.value && credit.value.balance || 0) < 1) {
    ElMessage.warning(t('proofs.noCredit'));
    return;
  }
  verifyTarget = row;
  verifyInput.value.click();
}

function onVerifyFilePicked(e) {
  const file = e.target.files && e.target.files[0];
  e.target.value = '';
  const row = verifyTarget;
  verifyTarget = null;
  if (!file || !row) return;
  doVerify(row, file);
}

/** 验真：重传原件与链上 hash 比对，扣 1 次 */
async function doVerify(row, file) {
  try {
    await ElMessageBox.confirm(t('proofs.verifyConfirm'), t('common.tip'), {
      type: 'warning',
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
    });
  } catch {
    return;
  }
  row.verifyLoading = true;
  try {
    const passed = await clientApi.verifyProof(row.id, file);
    await ElMessageBox.alert(
      t(passed ? 'proofs.verifyOk' : 'proofs.verifyFail'),
      t('common.tip'),
      {
        type: passed ? 'success' : 'warning',
        confirmButtonText: t('common.confirm'),
      }
    );
    useCredit().refresh();
  } catch (e) {
    ElMessage.error(e.message);
  } finally {
    row.verifyLoading = false;
  }
}

// ===== 列表 =====
async function reload() {
  listLoading.value = true;
  try {
    rows.value = await clientApi.listProofs();
  } catch (e) {
    ElMessage.error(e.message);
  } finally {
    listLoading.value = false;
  }
}
onMounted(reload);
</script>

<template>
  <div>
    <el-card class="upload-card">
      <template #header>
        <div class="card-title">
          {{ t('proofs.uploadTitle') }}
          <el-tag v-if="cooldown > 0" type="warning" effect="plain" class="cooldown-tag">
            {{ t('proofs.cooldown', { seconds: cooldown }) }}
          </el-tag>
        </div>
      </template>
      <el-alert :title="t('proofs.uploadTip', { seconds: UPLOAD_COOLDOWN_SECONDS })" type="info" :closable="false" style="margin-bottom: 12px" />
      <el-upload
        class="uploader"
        drag
        :auto-upload="false"
        :show-file-list="false"
        :disabled="cooldown > 0 || uploading"
        :on-change="onFileChange"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          {{ uploading ? t('proofs.uploading') : t('proofs.drop') }}
        </div>
      </el-upload>
    </el-card>

    <el-card class="list-card">
      <el-table :data="rows" v-loading="listLoading" empty-text="t('proofs.empty')">
        <el-table-column prop="fileName" :label="t('proofs.colFile')" min-width="180" show-overflow-tooltip />
        <el-table-column :label="t('proofs.colSize')" width="100">
          <template #default="{ row }">{{ formatBytes(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column :label="t('proofs.colHash')" width="170">
          <template #default="{ row }">
            <span class="mono" :title="row.fileHash">{{ shortHash(row.fileHash) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('proofs.colUploaded')" width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('proofs.colStatus')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'STORED' ? 'success' : 'warning'" effect="plain">
              {{ row.status === 'STORED' ? t('proofs.statusStored') : t('proofs.statusPending') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('proofs.colTx')" width="170">
          <template #default="{ row }">
            <a
              v-if="row.txHash"
              :href="EXPLORER_TX_URL + row.txHash"
              target="_blank"
              rel="noopener"
              class="mono"
              :title="row.txHash"
            >{{ shortHash(row.txHash) }}</a>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('proofs.colAction')" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              type="primary"
              size="small"
              :loading="row.storeLoading"
              @click="doStore(row)"
            >{{ t('proofs.store') }}</el-button>
            <el-button
              v-else
              size="small"
              :loading="row.verifyLoading"
              @click="clickVerify(row)"
            >{{ t('proofs.verify') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 校验重传原件用的隐藏文件选择器 -->
    <input ref="verifyInput" type="file" style="display: none" @change="onVerifyFilePicked" />
  </div>
</template>

<style scoped>
.upload-card { margin-bottom: 16px; }
.card-title { font-size: 15px; font-weight: 600; display: flex; align-items: center; gap: 8px; }
.cooldown-tag { font-weight: normal; }
.uploader :deep(.el-upload-dragger) { padding: 24px 0; }
.mono { font-family: Consolas, Menlo, monospace; font-size: 12px; }
</style>
